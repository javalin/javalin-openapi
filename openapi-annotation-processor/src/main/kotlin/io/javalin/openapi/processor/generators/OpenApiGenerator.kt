package io.javalin.openapi.processor.generators

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.javalin.http.HttpStatus
import io.javalin.openapi.*
import io.javalin.openapi.OpenApiOperation.AUTO_GENERATE
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.StructureType.ARRAY
import io.javalin.openapi.experimental.processor.generators.ExampleGenerator
import io.javalin.openapi.experimental.processor.shared.addIfNotEmpty
import io.javalin.openapi.experimental.processor.shared.addString
import io.javalin.openapi.experimental.processor.shared.computeIfAbsent
import io.javalin.openapi.experimental.processor.shared.getTypeMirror
import io.javalin.openapi.experimental.processor.shared.info
import io.javalin.openapi.experimental.processor.shared.saveResource
import io.javalin.openapi.experimental.processor.shared.toJsonArray
import io.javalin.openapi.experimental.processor.shared.toPrettyString
import io.javalin.openapi.processor.OpenApiAnnotationProcessor.Companion.context
import io.javalin.openapi.processor.generators.OpenApiGenerator.In.COOKIE
import io.javalin.openapi.processor.generators.OpenApiGenerator.In.FORM_DATA
import io.javalin.openapi.processor.generators.OpenApiGenerator.In.HEADER
import io.javalin.openapi.processor.generators.OpenApiGenerator.In.PATH
import io.javalin.openapi.processor.generators.OpenApiGenerator.In.QUERY
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
        val openApi = JsonObject()
        openApi.addProperty("openapi", "3.0.3")

        // fill info
        val info = JsonObject()
        info.addProperty("title", context.parameters.info.title)
        info.addProperty("version", context.parameters.info.version)
        openApi.add("info", info)

        // fill paths
        val paths = JsonObject()
        openApi.add("paths", paths)

        for ((openApiElement, routeAnnotation) in openApiAnnotations.sortedBy { it.second.getFormattedPath() }) {
            if (routeAnnotation.ignore) {
                continue
            }

            val path = paths.computeIfAbsent(routeAnnotation.getFormattedPath()) { JsonObject() }

            // https://swagger.io/specification/#paths-object
            for (method in routeAnnotation.methods.sortedBy { it.name }) {
                val operation = JsonObject()

                // General
                operation.add("tags", routeAnnotation.tags.toJsonArray())
                operation.addString("summary", routeAnnotation.summary)
                operation.addString("description", routeAnnotation.description)

                // ExternalDocs
                // ~ https://swagger.io/specification/#external-documentation-object
                // operation.addProperty("externalDocs", ); UNSUPPORTED

                // OperationId
                operation.addString("operationId", generateOperationId(method, routeAnnotation))

                // Parameters
                // ~ https://swagger.io/specification/#parameter-object
                val parameters = JsonArray()

                val parameterAnnotations = linkedMapOf(
                    COOKIE to routeAnnotation.cookies,
                    FORM_DATA to routeAnnotation.formParams,
                    HEADER to routeAnnotation.headers,
                    PATH to routeAnnotation.pathParams,
                    QUERY to routeAnnotation.queryParams
                )

                parameterAnnotations.forEach { (parameterType, annotations) ->
                    annotations.forEach { parameterAnnotation ->
                        parameters.add(fromParameter(parameterType, parameterAnnotation, explicit = true))
                    }
                }

                operation.add("parameters", parameters)

                // RequestBody
                // ~ https://swagger.io/specification/#request-body-object
                operation.addRequestBody(openApiElement, routeAnnotation.requestBody)

                // Responses
                // ~ https://swagger.io/specification/#responses-object
                operation.addResponses(openApiElement, routeAnnotation.responses)

                // Callbacks
                // ~ https://swagger.io/specification/#callback-object
                if (routeAnnotation.callbacks.isNotEmpty()) {
                    val callbacks = JsonObject()

                    routeAnnotation.callbacks.forEach { callback ->
                        val eventObject = JsonObject()
                        callbacks.add(callback.name, eventObject)

                        val urlObject = JsonObject()
                        eventObject.add(callback.url, urlObject)

                        val methodObject = JsonObject()
                        urlObject.add(callback.method.name.lowercase(), methodObject)

                        methodObject.addString("summary", callback.summary)
                        methodObject.addString("description", callback.description)
                        methodObject.addRequestBody(openApiElement, callback.requestBody)
                        methodObject.addResponses(openApiElement, callback.responses)
                    }

                    operation.add("callbacks", callbacks)
                }

                // Deprecated
                operation.addProperty("deprecated", routeAnnotation.deprecated)

                // Security
                // ~ https://swagger.io/specification/#security-requirement-object
                val security = JsonArray()

                for (securityAnnotation in routeAnnotation.security.sortedBy { it.name }) {
                    val securityEntry = JsonObject()
                    val scopes = JsonArray()

                    for (scopeAnnotation in securityAnnotation.scopes) {
                        scopes.add(scopeAnnotation)
                    }

                    securityEntry.add(securityAnnotation.name, scopes)
                    security.add(securityEntry)
                }

                operation.add("security", security)

                // servers
                path.add(method.name.lowercase(), operation)
            }
        }

        val components = JsonObject()
        val schemas = JsonObject()
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

                val (schema, references) = context.typeSchemaGenerator.createTypeSchema(componentReference, false)
                componentReferences.putAll(references.associateBy { it.fullName })
                generatedComponents[name] = componentReference to schema
            }
        }

        componentReferences.clear()

        generatedComponents
            .mapNotNull { it.value }
            .filter { (type, _) ->
                val alreadyExists = schemas.has(type.simpleName)

                context.inDebug {
                    if (alreadyExists) {
                        context.env.messager.info("Scheme component '${type.simpleName}' already exists. Generated scheme for ${type.fullName} won't be added to the OpenAPI document.")
                    }
                }

                !alreadyExists
            }
            .forEach { (type, schema) ->
                schemas.add(type.simpleName, schema)
            }

        components.add("schemas", schemas)
        openApi.add("components", components)

        return openApi.toPrettyString()
    }

    private fun JsonObject.addRequestBody(openApiElement: Element, requestBodyAnnotation: OpenApiRequestBody) {
        val requestBody = JsonObject()
        requestBody.addString("description", requestBodyAnnotation.description)
        requestBody.addContent(openApiElement, requestBodyAnnotation.content)
        addIfNotEmpty("requestBody", requestBody)
        requestBody.addProperty("required", requestBodyAnnotation.required)
    }

    private fun JsonObject.addResponses(openApiElement: Element, responseAnnotations: Array<OpenApiResponse>) {
        val responses = JsonObject()

        for (responseAnnotation in responseAnnotations.sortedBy { it.status }) {
            val response = JsonObject()

            val description = responseAnnotation.description
                .takeIf { it != NULL_STRING }
                ?: responseAnnotation
                    .status
                    .toIntOrNull()
                    ?.let { HttpStatus.forStatus(it) }
                    ?.message

            response.addString("description", description)
            response.addContent(openApiElement, responseAnnotation.content)
            responses.add(responseAnnotation.status, response)

            val headers = JsonObject()
            responseAnnotation.headers.forEach { headers.add(it.name, fromParameter(HEADER, it, explicit = false)) }
            response.addIfNotEmpty("headers", headers)
        }

        add("responses", responses)
    }

    enum class In(val identifier: String) {
        QUERY("query"),
        HEADER("header"),
        PATH("path"),
        COOKIE("cookie"),
        FORM_DATA("formData")
    }

    // Parameter
    // https://swagger.io/specification/#parameter-object
    private fun fromParameter(`in`: In, parameterInstance: OpenApiParam, explicit: Boolean): JsonObject {
        val parameter = JsonObject()

        if (explicit) {
            parameter.addString("name", parameterInstance.name)
            parameter.addString("in", `in`.identifier)
        }

        parameter.addString("description", parameterInstance.description)

        if (explicit || parameterInstance.required) {
            parameter.addProperty("required", parameterInstance.required)
        }

        if (explicit || parameterInstance.deprecated) {
            parameter.addProperty("deprecated", parameterInstance.deprecated)
        }

        if (explicit || parameterInstance.allowEmptyValue) {
            parameter.addProperty("allowEmptyValue", parameterInstance.allowEmptyValue)
        }

        val schema = createTypeDescriptionWithReferences(parameterInstance.getTypeMirror { type })

        if (parameterInstance.example.isNotEmpty()) {
            schema.addProperty("example", parameterInstance.example)
        }

        parameter.add("schema", schema)
        return parameter
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

    private fun JsonObject.addContent(element: Element, contentAnnotations: Array<OpenApiContent>) =
        context.inContext {
            val requestBodyContent = JsonObject()
            val requestBodySchemes = TreeMap<String, JsonObject>()

            for (contentAnnotation in contentAnnotations) {
                val (mimeType, mediaTypeSchema) =
                    contentAnnotation
                        .toData()
                        .toMimeTypeSchema(element, contentAnnotation)
                        ?: continue

                requestBodySchemes[mimeType] = mediaTypeSchema
            }

            requestBodySchemes.forEach { (mimeType, scheme) ->
                requestBodyContent.add(mimeType, scheme)
            }

            if (requestBodyContent.size() > 0) {
                add("content", requestBodyContent)
            }
        }

    private fun OpenApiContentData.toMimeTypeSchema(element: Element, source: Annotation): Pair<String, JsonObject>? =
        context.inContext {
            var contentData = this@toMimeTypeSchema
            val from = source.getTypeMirror { contentData.from() }

            if (contentData.mimeType == null) {
                contentData =
                    when (NULL_CLASS::class.qualifiedName) {
                        // Use 'type` as `mimeType` if there's no other mime-type declaration in @OpenApiContent annotation
                        // ~ https://github.com/javalin/javalin-openapi/issues/88
                        from.getFullName() -> contentData.copy(mimeType = type, type = null)
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

            val mediaType = JsonObject()
            val mediaTypeSchema = contentData.toTypeSchema(source)

            if (mediaTypeSchema.size() > 0) {
                mediaType.add("schema", mediaTypeSchema)
            }
            mediaType.addContentExample(contentData)

            return@inContext contentData.mimeType!! to mediaType
        }

    private fun JsonObject.addContentExample(contentData: OpenApiContentData) {
        if (contentData.example != null) {
            addProperty("example", contentData.example)
        }

        if (contentData.exampleObjects != null) {
            val generatorResult = ExampleGenerator.generateFromExamples(contentData.exampleObjects!!)

            when {
                generatorResult.simpleValue != null -> addProperty("example", generatorResult.simpleValue)
                generatorResult.jsonElement != null -> add("example", generatorResult.jsonElement)
            }
        }

    }

    private fun OpenApiContentData.toTypeSchema(source: Annotation): JsonObject = context.inContext {
        val from = source.getTypeMirror { from() }

        when {
            properties == null && additionalProperties == null && from.getFullName() != NULL_CLASS::class.java.name ->
                createTypeDescriptionWithReferences(from)
            properties == null && additionalProperties == null -> {
                val schema = JsonObject()
                type?.also { schema.addProperty("type", it) }
                format?.also { schema.addProperty("format", it) }
                schema
            }
            else -> {
                val schema = JsonObject()
                schema.addProperty("type", "object")

                if (properties != null) {
                    schema.add("properties", properties!!.toTypeSchema())
                }

                additionalProperties?.let {
                    val additionalPropertiesData = it.toData()
                    schema.add("additionalProperties", additionalPropertiesData.toTypeSchema(it))
                    schema.addContentExample(additionalPropertiesData)
                }
                schema
            }
        }
    }

    private fun Collection<OpenApiContentProperty>.toTypeSchema(): JsonObject =
        context.inContext {
            val propertiesSchema = JsonObject()

            for (contentProperty in this@toTypeSchema) {
                val propertyFormat = contentProperty.format.takeIf { it != NULL_STRING }

                val contentPropertyFrom = contentProperty.getTypeMirror { contentProperty.from }
                val propertyScheme = if (contentPropertyFrom.getFullName() != NULL_CLASS::class.java.name) {
                    createTypeDescriptionWithReferences(contentPropertyFrom)
                } else {
                    JsonObject().apply {
                        addProperty("type", contentProperty.type)
                        propertyFormat?.let { addProperty("format", it) }
                    }
                }

                propertiesSchema.add(contentProperty.name,
                    if (contentProperty.isArray) {
                        // wrap into OpenAPI array object
                        JsonObject().apply {
                            addProperty("type", "array")
                            add("items", propertyScheme)
                        }
                    } else {
                        propertyScheme
                    }
                )
            }

            propertiesSchema
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
