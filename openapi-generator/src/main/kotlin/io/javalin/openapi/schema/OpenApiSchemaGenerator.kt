package io.javalin.openapi.schema

import io.javalin.introspection.ClassDefinition
import io.javalin.openapi.NULL_STRING
import io.javalin.openapi.OpenApiStatus
import io.javalin.openapi.OpenApiOperation.AUTO_GENERATE
import io.javalin.openapi.experimental.OpenApiType
import io.javalin.openapi.experimental.SchemaGenerationContext
import io.javalin.openapi.experimental.StructureType.ARRAY
import io.javalin.openapi.experimental.processor.generators.ExampleGenerator
import io.javalin.openapi.experimental.processor.generators.ResultScheme
import java.util.Locale
import java.util.TreeMap

class OpenApiSchemaGenerator(
    private val context: SchemaGenerationContext,
    private val title: String,
    private val version: String,
    private val defaultStatusDescription: (String) -> String? = { OpenApiStatus.reasonPhrase(it) },
) {

    fun generateSchema(routes: List<Map<String, Any?>>): String {
        return generateRouteSchema(routes.map(OpenApiRouteDefinition::from))
    }

    private fun generateRouteSchema(routes: List<OpenApiRouteDefinition>): String {
        val schema =
            OpenApiSchemaBuilder()
                .openApiVersion("3.1.0")
                .info { it.title(title).version(version) }

        for (route in routes.sortedBy { it.formattedPath }) {
            if (route.ignore) {
                continue
            }

            val pathBuilder = schema.path(route.formattedPath)

            for (method in route.methods.sorted()) {
                pathBuilder.operation(method.lowercase()) {
                    tags(route.tags)
                    summary(route.summary)
                    description(route.description)
                    operationId(generateOperationId(method, route).takeIf { it != NULL_STRING })

                    buildParameters(route)
                    buildRequestBody(route.requestBody)
                    buildResponses(route.responses)
                    buildCallbacks(route.callbacks)

                    if (route.deprecated) {
                        deprecated(true)
                    }

                    val securities = route.security
                    if (securities.isNotEmpty()) {
                        security {
                            for (security in securities.sortedBy { it.name }) {
                                securityRequirement(security.name, *security.scopes.toTypedArray())
                            }
                        }
                    }
                }
            }
        }

        schema.resolveComponentReferences { type -> context.typeSchemaGenerator.createTypeSchema(type, false) }
        return schema.toJson()
    }

    fun generateVersionedSchemas(routes: List<Map<String, Any?>>): Map<String, String> =
        generateVersionedRouteSchemas(routes.map(OpenApiRouteDefinition::from))

    private fun generateVersionedRouteSchemas(routes: List<OpenApiRouteDefinition>): Map<String, String> =
        routes
            .flatMap { route -> route.versions.map { version -> version to route } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, versionRoutes) -> generateRouteSchema(versionRoutes.toSet().toList()) }

    private fun OperationBuilder.buildParameters(route: OpenApiRouteDefinition) {
        parameters {
            val parametersByLocation = linkedMapOf(
                In.COOKIE to route.cookies,
                In.HEADER to route.headers,
                In.PATH to route.pathParameters,
                In.QUERY to route.queryParameters,
            )

            parametersByLocation.forEach { (location, parameters) ->
                parameters.forEach { parameter ->
                    parameter(
                        name = parameter.name,
                        location = location.identifier,
                        schema = createTypeDescriptionWithReferences(parameter.type),
                        description = parameter.description,
                        required = parameter.required || location == In.PATH,
                        deprecated = parameter.deprecated,
                        allowEmptyValue = parameter.allowEmptyValue,
                        example = parameter.example,
                    )
                }
            }
        }
    }

    private fun OperationBuilder.buildRequestBody(requestBody: OpenApiRequestBodyDefinition?) {
        if (requestBody == null) {
            return
        }
        requestBody {
            description(requestBody.description)
            content { addResolvedContent(requestBody.content) }
            if (requestBody.required) { required(true) }
        }
    }

    private fun OperationBuilder.buildResponses(responses: List<OpenApiResponseDefinition>) {
        responses {
            for (response in responses.sortedBy { it.status }) {
                response(response.status) {
                    description(descriptionOf(response))
                    content { addResolvedContent(response.content) }
                    headers {
                        response.headers.forEach { header ->
                            header(
                                name = header.name,
                                schema = createTypeDescriptionWithReferences(header.type),
                                description = header.description,
                                required = header.required,
                                deprecated = header.deprecated,
                                allowEmptyValue = header.allowEmptyValue,
                                example = header.example,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun OperationBuilder.buildCallbacks(callbacks: List<OpenApiCallbackDefinition>) {
        if (callbacks.isEmpty()) {
            return
        }

        callbacks {
            callbacks.forEach { callback ->
                callback(
                    name = callback.name,
                    url = callback.url,
                    method = callback.method.lowercase()
                ) {
                    summary(callback.summary)
                    description(callback.description)
                    val callbackBody = callback.requestBody
                    requestBody {
                        description(callbackBody?.description)
                        content { addResolvedContent(callbackBody?.content.orEmpty()) }
                        if (callbackBody?.required == true) { required(true) }
                    }
                    responses {
                        for (response in callback.responses.sortedBy { it.status }) {
                            response(response.status) {
                                description(descriptionOf(response))
                                content { addResolvedContent(response.content) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ContentBuilder.addResolvedContent(contents: List<OpenApiContentDefinition>) {
        val resolvedEntries = TreeMap<String, MediaTypeBuilder.() -> Unit>()

        for (content in contents) {
            val resolved = resolveMediaType(content) ?: continue
            resolvedEntries[resolved.first] = resolved.second
        }

        resolvedEntries.forEach { (mimeType, configure) -> mediaType(mimeType, configure) }
    }

    enum class In(val identifier: String) {
        QUERY("query"),
        HEADER("header"),
        PATH("path"),
        COOKIE("cookie"),
    }

    private fun generateOperationId(method: String, route: OpenApiRouteDefinition, pathParamPrefix: String = "By"): String =
        when (val operationId = route.operationId) {
            AUTO_GENERATE ->
                method.lowercase() + route.path.split('/')
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
                    .joinToString(separator = "") {
                        it.split('-').joinToString(separator = "") { it.capitalise() }
                    }
            else -> operationId
        }

    private fun String.capitalise(): String = this.replaceFirstChar {
        it.titlecase(Locale.getDefault())
    }

    private fun resolveMediaType(content: OpenApiContentDefinition): Pair<String, MediaTypeBuilder.() -> Unit>? {
        val from = content.from
        val fromIsNull = !content.hasResolvedSource
        var type = content.type
        var mimeType = content.mimeType

        if (mimeType == null) {
            if (fromIsNull) {
                mimeType = type
                type = null
            } else {
                mimeType = detectContentType(from!!)
            }
        }

        if (mimeType == null) {
            context.reportWarning(
                """
                OpenApi generator cannot find matching mime type defined.
                Content:
                    $content
                """.trimIndent()
            )
            return null
        }

        val resolvedType = type
        val format = content.format
        val properties = content.properties.takeIf { it.isNotEmpty() }
        val additionalProperties = content.additionalProperties

        val configure: MediaTypeBuilder.() -> Unit = {
            when {
                properties == null && additionalProperties == null && !fromIsNull ->
                    schema(createTypeDescriptionWithReferences(from!!))

                properties == null && additionalProperties == null ->
                    schema {
                        resolvedType?.let { type(it) }
                        format?.let { format(it) }
                    }

                else -> objectSchema {
                    properties?.let { buildProperties(it) }
                    additionalProperties?.let { buildAdditionalProperties(it) }
                }
            }

            applyExample(content)
        }

        return mimeType to configure
    }

    private fun ExampleHolder.applyExample(content: OpenApiContentDefinition) {
        content.example?.let { example(it) }
        content.exampleObjects.takeIf { it.isNotEmpty() }?.let { examples ->
            val result = ExampleGenerator.generateFromExamples(examples)
            result.simpleValue?.let { example(it) }
            result.jsonElement?.let { exampleJson(it) }
        }
    }

    private fun ObjectSchemaBuilder.buildProperties(properties: List<OpenApiContentPropertyDefinition>) {
        for (property in properties) {
            val from = property.from?.takeIf { property.hasResolvedSource }

            if (property.isArray) {
                if (from != null) {
                    arrayProperty(property.name, createTypeDescriptionWithReferences(from))
                } else {
                    arrayProperty(property.name, property.type, property.format)
                }
            } else {
                if (from != null) {
                    property(property.name, createTypeDescriptionWithReferences(from))
                } else {
                    property(property.name, property.type, property.format)
                }
            }
        }
    }

    private fun ObjectSchemaBuilder.buildAdditionalProperties(additionalProperties: OpenApiContentDefinition) {
        val from = additionalProperties.from?.takeIf { additionalProperties.hasResolvedSource }

        if (from != null) {
            additionalProperties(createTypeDescriptionWithReferences(from))
        } else {
            additionalProperties(additionalProperties.type, additionalProperties.format)
        }

        applyExample(additionalProperties)
    }

    private fun detectContentType(from: ClassDefinition): String {
        val model = context.toOpenApiType(from)

        return when {
            (model.structureType == ARRAY && model.simpleName == "Byte") || model.simpleName == "[B" || model.simpleName == "File" -> "application/octet-stream"
            model.structureType == ARRAY -> "application/json"
            model.simpleName == "String" -> "text/plain"
            else -> "application/json"
        }
    }

    private fun createTypeDescriptionWithReferences(from: ClassDefinition): ResultScheme {
        val model: OpenApiType = context.toOpenApiType(from)
        return context.typeSchemaGenerator.createEmbeddedTypeDescription(model)
    }

    private fun descriptionOf(response: OpenApiResponseDefinition): String =
        response.description
            ?: defaultStatusDescription(response.status)
            ?: ""

}
