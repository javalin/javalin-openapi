package io.javalin.openapi.processor.generators

import io.javalin.http.HttpStatus
import io.javalin.openapi.*
import io.javalin.openapi.OpenApiOperation.AUTO_GENERATE
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.StructureType.ARRAY
import com.google.gson.JsonObject
import io.javalin.openapi.experimental.processor.generators.ExampleGenerator
import io.javalin.openapi.experimental.processor.generators.toExampleProperty
import io.javalin.openapi.experimental.processor.shared.getTypeMirror
import io.javalin.openapi.experimental.processor.shared.info
import io.javalin.openapi.experimental.processor.shared.saveResource
import io.javalin.openapi.processor.OpenApiAnnotationProcessor.Companion.context
import io.javalin.openapi.processor.generators.OpenApiGenerator.In.COOKIE
import io.javalin.openapi.processor.generators.OpenApiGenerator.In.FORM_DATA
import io.javalin.openapi.processor.generators.OpenApiGenerator.In.HEADER
import io.javalin.openapi.processor.generators.OpenApiGenerator.In.PATH
import io.javalin.openapi.processor.generators.OpenApiGenerator.In.QUERY
import io.javalin.openapi.schema.*
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import java.util.Locale
import java.util.TreeMap
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic
import javax.tools.Diagnostic.Kind.WARNING

internal class OpenApiGenerator {

    private val componentReferences = mutableMapOf<String, ClassDefinition>()

    fun generate(roundEnvironment: RoundEnvironment) {
        val aggregatedOpenApiAnnotations = roundEnvironment.getElementsAnnotatedWith(OpenApis::class.java)
            .flatMap { element ->
                element.getAnnotation(OpenApis::class.java)
                    .value
                    .asSequence()
                    .map { element to it }
            }

        val standaloneOpenApiAnnotations = roundEnvironment.getElementsAnnotatedWith(OpenApi::class.java)
            .map { it to it.getAnnotation(OpenApi::class.java) }

        val openApiAnnotationsByVersion = (aggregatedOpenApiAnnotations + standaloneOpenApiAnnotations)
            .flatMap { it.second.versions.map { version -> version to it } }
            .groupBy { (version, _) -> version }
            .mapValues { (_, annotations) -> annotations.map { it.second } }

        openApiAnnotationsByVersion
            .map { (version, openApiAnnotations) ->
                val preparedOpenApiAnnotations = openApiAnnotations.toSet()
                val generatedOpenApiSchema = generateSchema(preparedOpenApiAnnotations)

                val resourceName = "openapi-${version.replace(" ", "-")}.json"
                val resource = context.env.filer.saveResource(context, "openapi-plugin/$resourceName", generatedOpenApiSchema)
                    ?.toUri()
                    ?.toString()
                    ?: return

                if (context.configuration.validateWithParser) {
                    val parsedSchema = OpenAPIV3Parser().readLocation(resource, emptyList(), ParseOptions())

                    if (parsedSchema.messages.size > 0) {
                        context.env.messager.printMessage(Diagnostic.Kind.NOTE, "OpenApi Validation Warnings :: ${parsedSchema.messages.size}")
                    }

                    parsedSchema.messages.forEach {
                        context.env.messager.printMessage(WARNING, it)
                    }
                }

                resourceName
            }
            .joinToString(separator = "\n")
            .let { context.env.filer.saveResource(context, "openapi-plugin/.index", it) }
    }

    /**
     * Based on https://swagger.io/specification/
     *
     * @param openApiAnnotations annotation instances to map
     * @return OpenApi JSON response
     */
    private fun generateSchema(openApiAnnotations: Collection<Pair<Element, OpenApi>>): String {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = context.parameters.info.title, version = context.parameters.info.version)

        for ((openApiElement, routeAnnotation) in openApiAnnotations.sortedBy { it.second.getFormattedPath() }) {
            if (routeAnnotation.ignore) {
                continue
            }

            val pathBuilder = schema.path(routeAnnotation.getFormattedPath())

            // https://swagger.io/specification/#paths-object
            for (method in routeAnnotation.methods.sortedBy { it.name }) {
                pathBuilder.operation(method.name.lowercase()) {
                    // General
                    tags(routeAnnotation.tags.toList())
                    summary(routeAnnotation.summary.takeIf { it != NULL_STRING })
                    description(routeAnnotation.description.takeIf { it != NULL_STRING })

                    // ExternalDocs
                    // ~ https://swagger.io/specification/#external-documentation-object
                    // UNSUPPORTED

                    // OperationId
                    operationId(generateOperationId(method, routeAnnotation).takeIf { it != NULL_STRING })

                    // Parameters
                    // ~ https://swagger.io/specification/#parameter-object
                    buildParameters(routeAnnotation)

                    // RequestBody
                    // ~ https://swagger.io/specification/#request-body-object
                    buildRequestBody(openApiElement, routeAnnotation.requestBody)

                    // Responses
                    // ~ https://swagger.io/specification/#responses-object
                    buildResponses(openApiElement, routeAnnotation.responses)

                    // Callbacks
                    // ~ https://swagger.io/specification/#callback-object
                    buildCallbacks(openApiElement, routeAnnotation.callbacks)

                    // Deprecated
                    deprecated(routeAnnotation.deprecated)

                    // Security
                    // ~ https://swagger.io/specification/#security-requirement-object
                    security {
                        for (securityAnnotation in routeAnnotation.security.sortedBy { it.name }) {
                            securityRequirement(securityAnnotation.name, *securityAnnotation.scopes)
                        }
                    }
                }
            }
        }

        resolveComponentReferences(schema)
        return schema.toJson()
    }

    private fun OperationBuilder.buildParameters(routeAnnotation: OpenApi) {
        parameters {
            val parameterAnnotations = linkedMapOf(
                COOKIE to routeAnnotation.cookies,
                FORM_DATA to routeAnnotation.formParams,
                HEADER to routeAnnotation.headers,
                PATH to routeAnnotation.pathParams,
                QUERY to routeAnnotation.queryParams
            )

            parameterAnnotations.forEach { (parameterType, annotations) ->
                annotations.forEach { parameterAnnotation ->
                    val paramSchema = createTypeDescriptionWithReferences(parameterAnnotation.getTypeMirror { type })
                    parameter(
                        name = parameterAnnotation.name,
                        location = parameterType.identifier,
                        schema = paramSchema,
                        description = parameterAnnotation.description.takeIf { it != NULL_STRING },
                        required = parameterAnnotation.required,
                        deprecated = parameterAnnotation.deprecated,
                        allowEmptyValue = parameterAnnotation.allowEmptyValue,
                        example = parameterAnnotation.example.takeIf { it.isNotEmpty() },
                    )
                }
            }
        }
    }

    private fun OperationBuilder.buildRequestBody(element: Element, annotation: OpenApiRequestBody) {
        requestBody {
            description(annotation.description.takeIf { it != NULL_STRING })
            content { addResolvedContent(element, annotation.content) }
            required(annotation.required)
        }
    }

    private fun OperationBuilder.buildResponses(element: Element, responseAnnotations: Array<OpenApiResponse>) {
        responses {
            for (responseAnnotation in responseAnnotations.sortedBy { it.status }) {
                response(responseAnnotation.status) {
                    val desc = responseAnnotation.description
                        .takeIf { it != NULL_STRING }
                        ?: responseAnnotation
                            .status
                            .toIntOrNull()
                            ?.let { HttpStatus.forStatus(it) }
                            ?.message

                    description(desc)
                    content { addResolvedContent(element, responseAnnotation.content) }
                    headers {
                        responseAnnotation.headers.forEach { headerParam ->
                            val headerSchema = createTypeDescriptionWithReferences(headerParam.getTypeMirror { type })
                            header(
                                name = headerParam.name,
                                schema = headerSchema,
                                description = headerParam.description.takeIf { it != NULL_STRING },
                                required = headerParam.required,
                                deprecated = headerParam.deprecated,
                                allowEmptyValue = headerParam.allowEmptyValue,
                                example = headerParam.example.takeIf { it.isNotEmpty() },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun OperationBuilder.buildCallbacks(element: Element, callbackAnnotations: Array<OpenApiCallback>) {
        if (callbackAnnotations.isEmpty()) {
            return
        }

        callbacks {
            callbackAnnotations.forEach { callbackAnnotation ->
                callback(
                    name = callbackAnnotation.name,
                    url = callbackAnnotation.url,
                    method = callbackAnnotation.method.name.lowercase()
                ) {
                    summary(callbackAnnotation.summary.takeIf { it != NULL_STRING })
                    description(callbackAnnotation.description.takeIf { it != NULL_STRING })
                    requestBody {
                        description(callbackAnnotation.requestBody.description.takeIf { it != NULL_STRING })
                        content { addResolvedContent(element, callbackAnnotation.requestBody.content) }
                        required(callbackAnnotation.requestBody.required)
                    }
                    responses {
                        for (responseAnnotation in callbackAnnotation.responses.sortedBy { it.status }) {
                            response(responseAnnotation.status) {
                                val desc = responseAnnotation.description
                                    .takeIf { it != NULL_STRING }
                                    ?: responseAnnotation
                                        .status
                                        .toIntOrNull()
                                        ?.let { HttpStatus.forStatus(it) }
                                        ?.message

                                description(desc)
                                content { addResolvedContent(element, responseAnnotation.content) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ContentBuilder.addResolvedContent(element: Element, contentAnnotations: Array<OpenApiContent>) {
        val resolvedEntries = TreeMap<String, MediaTypeBuilder.() -> Unit>()

        for (contentAnnotation in contentAnnotations) {
            val resolved = resolveMediaType(element, contentAnnotation) ?: continue
            resolvedEntries[resolved.first] = resolved.second
        }

        resolvedEntries.forEach { (mimeType, configure) -> mediaType(mimeType, configure) }
    }

    private fun resolveComponentReferences(schema: OpenApiSchemaBuilder) {
        val generatedComponents = TreeMap<String, Pair<ClassDefinition, JsonObject>?> { a, b -> a.compareTo(b) }

        while (generatedComponents.size < componentReferences.size) {
            for ((name, componentReference) in componentReferences.toMutableMap()) {
                if (generatedComponents.containsKey(name)) {
                    continue
                }

                if (componentReference.fullName == "java.lang.Object") {
                    generatedComponents[name] = null
                    continue
                }

                val (json, references) = context.typeSchemaGenerator.createTypeSchema(componentReference, false)
                componentReferences.putAll(references.associateBy { it.fullName })
                generatedComponents[name] = componentReference to json
            }
        }

        componentReferences.clear()

        generatedComponents
            .mapNotNull { it.value }
            .filter { (type, _) ->
                val alreadyExists = schema.hasComponentSchema(type.simpleName)

                context.inDebug {
                    if (alreadyExists) {
                        context.env.messager.info("Scheme component '${type.simpleName}' already exists. Generated scheme for ${type.fullName} won't be added to the OpenAPI document.")
                    }
                }

                !alreadyExists
            }
            .forEach { (type, json) ->
                schema.addComponentSchema(type.simpleName, json)
            }
    }

    enum class In(val identifier: String) {
        QUERY("query"),
        HEADER("header"),
        PATH("path"),
        COOKIE("cookie"),
        FORM_DATA("formData")
    }

    /**
     * Generates a operationId for the given [HttpMethod] and [OpenApi]-route.
     * The pattern used is `methodPathPartsCamelCaseByPathParam`.
     *
     * @param httpMethod The HTTP method to use. Prefixes the operationId
     * @param openApi The route to generate the operation id for
     * @param pathParamPrefix The prefix for path parameters to use. Defaults to `By`
     */
    private fun generateOperationId(
        httpMethod: HttpMethod,
        openApi: OpenApi,
        pathParamPrefix: String = "By"
    ): String =
        when (openApi.operationId) {
            AUTO_GENERATE -> {
                httpMethod.name.lowercase() + openApi.path.split('/')
                    .map { pathPart ->
                        if (pathPart.startsWith('{') or pathPart.startsWith('<')) {
                            /* Case this is a path parameter */
                            val pathParam = pathPart
                                .drop(1)
                                .dropLast(1)
                                .split('-')
                                .joinToString(separator = "") { it.capitalise() }
                            pathParamPrefix + pathParam
                        } else {
                            /* Case this is a regular part of the path */
                            pathPart.capitalise()
                        }
                    }
                    .toList()
                    .joinToString(separator = "") {
                        it.split('-').joinToString(separator = "") { it.capitalise() }
                    }
            }
            else -> openApi.operationId
        }

    /**
     * String extension for capitalisation, since it's often used during operationId generation.
     */
    private fun String.capitalise(): String = this.replaceFirstChar {
        it.titlecase(Locale.getDefault())
    }

    private fun resolveMediaType(element: Element, source: OpenApiContent): Pair<String, MediaTypeBuilder.() -> Unit>? =
        context.inContext {
            var contentData = source.toData()
            val from = source.getTypeMirror { contentData.from() }

            if (contentData.mimeType == null) {
                contentData =
                    when (NULL_CLASS::class.qualifiedName) {
                        // Use 'type` as `mimeType` if there's no other mime-type declaration in @OpenApiContent annotation
                        // ~ https://github.com/javalin/javalin-openapi/issues/88
                        from.getFullName() -> contentData.copy(mimeType = contentData.type, type = null)
                        else -> contentData.copy(mimeType = detectContentType(from))
                    }
            }

            if (contentData.mimeType == null) {
                val trees = context.trees

                if (trees != null) {
                    val compilationUnit = trees.getPath(element).compilationUnit
                    val tree = trees.getTree(element)
                    val startPosition = trees.sourcePositions.getStartPosition(compilationUnit, tree)

                    context.env.messager.printMessage(
                        WARNING,
                        """
                        OpenApi generator cannot find matching mime type defined.
                        Source:
                            Annotation in ${compilationUnit.lineMap.getLineNumber(startPosition)} at ${compilationUnit.sourceFile.name} line
                        Annotation:
                            $source
                        """.trimIndent()
                    )
                }

                return@inContext null
            }

            val resolvedContentData = contentData
            val fromMirror = source.getTypeMirror { resolvedContentData.from() }

            val configure: MediaTypeBuilder.() -> Unit = {
                when (resolvedContentData.properties) {
                    null if resolvedContentData.additionalProperties == null && fromMirror.getFullName() != NULL_CLASS::class.java.name ->
                        schema(createTypeDescriptionWithReferences(fromMirror))

                    null if resolvedContentData.additionalProperties == null ->
                        simpleSchema(resolvedContentData.type, resolvedContentData.format)

                    else -> objectSchema {
                        resolvedContentData.properties?.let { buildProperties(it) }
                        resolvedContentData.additionalProperties?.let { buildAdditionalProperties(it) }
                    }
                }

                applyExample(resolvedContentData)
            }

            return@inContext resolvedContentData.mimeType!! to configure
        }

    private fun ExampleHolder.applyExample(contentData: OpenApiContentData) {
        if (contentData.example != null) {
            example(contentData.example!!)
        }

        if (contentData.exampleObjects != null) {
            val generatorResult = ExampleGenerator.generateFromExamples(contentData.exampleObjects!!.map { it.toExampleProperty() })

            when {
                generatorResult.simpleValue != null -> example(generatorResult.simpleValue!!)
                generatorResult.jsonElement != null -> exampleJson(generatorResult.jsonElement!!)
            }
        }
    }

    private fun ObjectSchemaBuilder.buildProperties(properties: List<OpenApiContentProperty>) {
        context.inContext {
            for (contentProperty in properties) {
                val propertyFormat = contentProperty.format.takeIf { it != NULL_STRING }
                val contentPropertyFrom = contentProperty.getTypeMirror { contentProperty.from }
                val isResolved = contentPropertyFrom.getFullName() != NULL_CLASS::class.java.name

                if (contentProperty.isArray) {
                    if (isResolved) {
                        arrayProperty(contentProperty.name, createTypeDescriptionWithReferences(contentPropertyFrom))
                    } else {
                        arrayProperty(contentProperty.name, contentProperty.type, propertyFormat)
                    }
                } else {
                    if (isResolved) {
                        property(contentProperty.name, createTypeDescriptionWithReferences(contentPropertyFrom))
                    } else {
                        property(contentProperty.name, contentProperty.type, propertyFormat)
                    }
                }
            }
        }
    }

    private fun ObjectSchemaBuilder.buildAdditionalProperties(annotation: OpenApiAdditionalContent) {
        context.inContext {
            val additionalData = annotation.toData()
            val from = annotation.getTypeMirror { additionalData.from() }

            if (from.getFullName() != NULL_CLASS::class.java.name) {
                additionalProperties(createTypeDescriptionWithReferences(from))
            } else {
                additionalProperties(additionalData.type, additionalData.format)
            }

            applyExample(additionalData)
        }
    }

    private fun detectContentType(typeMirror: TypeMirror): String =
        context.inContext {
            val model = typeMirror.toClassDefinition()

            when {
                (model.structureType == ARRAY && model.simpleName == "Byte") || model.simpleName == "[B" || model.simpleName == "File" -> "application/octet-stream"
                model.structureType == ARRAY -> "application/json"
                model.simpleName == "String" -> "text/plain"
                else -> "application/json"
            }
        }

    private fun createTypeDescriptionWithReferences(type: TypeMirror): JsonObject =
        context.inContext {
            val model = type.toClassDefinition()
            val (json, references) = context.typeSchemaGenerator.createEmbeddedTypeDescription(model)
            componentReferences.putAll(references.associateBy { it.fullName })
            json
        }

}
