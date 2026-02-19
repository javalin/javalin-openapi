package io.javalin.openapi.schema

import io.javalin.openapi.*
import io.javalin.openapi.OpenApiOperation.AUTO_GENERATE
import io.javalin.openapi.experimental.AnnotationProcessorContext
import io.javalin.openapi.experimental.StructureType.ARRAY
import io.javalin.openapi.experimental.mirror
import io.javalin.openapi.experimental.processor.generators.ResultScheme
import io.javalin.openapi.experimental.processor.shared.getTypeMirror
import java.util.Locale
import java.util.TreeMap
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic.Kind.WARNING

class OpenApiSchemaGenerator(
    private val context: AnnotationProcessorContext,
    private val defaultStatusDescription: (String) -> String? = { null },
) {

    /**
     * Based on https://swagger.io/specification/
     *
     * @param openApiAnnotations annotation instances to map
     * @return OpenApi JSON response
     */
    fun generateSchema(openApiAnnotations: Collection<Pair<Element, OpenApi>>): String {
        val schema =
            OpenApiSchemaBuilder()
                .openApiVersion("3.0.3")
                .info(
                    title = context.parameters.info.title,
                    version = context.parameters.info.version,
                )

        for ((openApiElement, routeAnnotation) in openApiAnnotations.sortedBy { it.second.getFormattedPath() }) {
            if (routeAnnotation.ignore) {
                continue
            }

            // https://swagger.io/specification/#paths-object
            val pathBuilder = schema.path(routeAnnotation.getFormattedPath())

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

        schema.resolveComponentReferences { type -> context.typeSchemaGenerator.createTypeSchema(type, false) }
        return schema.toJson()
    }

    private fun OperationBuilder.buildParameters(routeAnnotation: OpenApi) {
        parameters {
            val parameterAnnotations = linkedMapOf(
                In.COOKIE to routeAnnotation.cookies,
                In.FORM_DATA to routeAnnotation.formParams,
                In.HEADER to routeAnnotation.headers,
                In.PATH to routeAnnotation.pathParams,
                In.QUERY to routeAnnotation.queryParams
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
                        ?: defaultStatusDescription(responseAnnotation.status)

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
                                    ?: defaultStatusDescription(responseAnnotation.status)

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

    enum class In(val identifier: String) {
        QUERY("query"),
        HEADER("header"),
        PATH("path"),
        COOKIE("cookie"),
        FORM_DATA("formData")
    }

    private fun generateOperationId(
        httpMethod: HttpMethod,
        openApi: OpenApi,
        pathParamPrefix: String = "By"
    ): String =
        when (openApi.operationId) {
            AUTO_GENERATE -> {
                httpMethod.name.lowercase() + openApi.path.split('/')
                    .map { pathPart ->
                        if (pathPart.startsWith('{') || pathPart.startsWith('<')) {
                            val pathParam = pathPart
                                .drop(1)
                                .dropLast(1)
                                .split('-')
                                .joinToString(separator = "") { it.capitalise() }
                            pathParamPrefix + pathParam
                        } else {
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
            applyExamples(contentData.exampleObjects!!)
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

    private fun createTypeDescriptionWithReferences(type: TypeMirror): ResultScheme =
        context.inContext {
            val model = type.toClassDefinition()
            context.typeSchemaGenerator.createEmbeddedTypeDescription(model)
        }

}
