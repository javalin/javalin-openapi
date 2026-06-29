package io.javalin.openapi.schema

import io.javalin.introspection.ClassDefinition
import io.javalin.openapi.ContentType
import io.javalin.openapi.OpenApiStatus
import io.javalin.openapi.NULL_CLASS
import io.javalin.openapi.NULL_STRING
import io.javalin.openapi.OpenApiOperation.AUTO_GENERATE
import io.javalin.openapi.experimental.OpenApiType
import io.javalin.openapi.experimental.SchemaGenerationContext
import io.javalin.openapi.experimental.StructureType.ARRAY
import io.javalin.openapi.experimental.processor.generators.ResultScheme
import java.util.Locale
import java.util.TreeMap

class OpenApiSchemaGenerator(
    private val context: SchemaGenerationContext,
    private val title: String,
    private val version: String,
    private val defaultStatusDescription: (String) -> String? = { OpenApiStatus.reasonPhrase(it) },
) {

    private val nullClassName = NULL_CLASS::class.java.name

    fun generateSchema(routes: List<Map<String, Any?>>): String {
        val schema =
            OpenApiSchemaBuilder()
                .openApiVersion("3.1.0")
                .info { it.title(title).version(version) }

        for (route in routes.sortedBy { it.formattedPath() }) {
            if (route["ignore"] as? Boolean == true) {
                continue
            }

            val pathBuilder = schema.path(route.formattedPath())

            for (method in route.texts("methods").sorted()) {
                pathBuilder.operation(method.lowercase()) {
                    setTags(route.texts("tags"))
                    summary(route.text("summary"))
                    description(route.text("description"))
                    operationId(generateOperationId(method, route).takeIf { it != NULL_STRING })

                    buildParameters(route)
                    buildRequestBody(route.child("requestBody"))
                    buildResponses(route.maps("responses"))
                    buildCallbacks(route.maps("callbacks"))

                    if (route["deprecated"] as? Boolean == true) {
                        deprecated(true)
                    }

                    val securities = route.maps("security")
                    if (securities.isNotEmpty()) {
                        security {
                            for (security in securities.sortedBy { it["name"] as String }) {
                                securityRequirement(security["name"] as String, *security.texts("scopes").toTypedArray())
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
        routes
            .flatMap { route -> route.texts("versions").map { version -> version to route } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, versionRoutes) -> generateSchema(versionRoutes.toSet().toList()) }

    private fun OperationBuilder.buildParameters(route: Map<String, Any?>) {
        parameters {
            val parametersByLocation = linkedMapOf(
                In.COOKIE to route.maps("cookies"),
                In.HEADER to route.maps("headers"),
                In.PATH to route.maps("pathParams"),
                In.QUERY to route.maps("queryParams"),
            )

            parametersByLocation.forEach { (location, parameters) ->
                parameters.forEach { parameter ->
                    parameter(
                        name = parameter["name"] as String,
                        location = location.identifier,
                        schema = createTypeDescriptionWithReferences(parameter["type"] as ClassDefinition),
                        description = parameter.text("description"),
                        required = (parameter["required"] as? Boolean ?: false) || location == In.PATH,
                        deprecated = parameter["deprecated"] as? Boolean ?: false,
                        allowEmptyValue = parameter["allowEmptyValue"] as? Boolean ?: false,
                        example = parameter.exampleText(),
                    )
                }
            }
        }
    }

    private fun OperationBuilder.buildRequestBody(requestBody: Map<String, Any?>?) {
        if (requestBody == null) {
            return
        }
        requestBody {
            description(requestBody.text("description"))
            content { addResolvedContent(requestBody.maps("content")) }
            if (requestBody["required"] as? Boolean == true) { required(true) }
        }
    }

    private fun OperationBuilder.buildResponses(responses: List<Map<String, Any?>>) {
        responses {
            for (response in responses.sortedBy { it["status"] as String }) {
                response(response["status"] as String) {
                    description(descriptionOf(response))
                    content { addResolvedContent(response.maps("content")) }
                    headers {
                        response.maps("headers").forEach { header ->
                            header(
                                name = header["name"] as String,
                                schema = createTypeDescriptionWithReferences(header["type"] as ClassDefinition),
                                description = header.text("description"),
                                required = header["required"] as? Boolean ?: false,
                                deprecated = header["deprecated"] as? Boolean ?: false,
                                allowEmptyValue = header["allowEmptyValue"] as? Boolean ?: false,
                                example = header.exampleText(),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun OperationBuilder.buildCallbacks(callbacks: List<Map<String, Any?>>) {
        if (callbacks.isEmpty()) {
            return
        }

        callbacks {
            callbacks.forEach { callback ->
                callback(
                    name = callback["name"] as String,
                    url = callback["url"] as String,
                    method = (callback["method"] as String).lowercase()
                ) {
                    summary(callback.text("summary"))
                    description(callback.text("description"))
                    val callbackBody = callback.child("requestBody")
                    requestBody {
                        description(callbackBody?.text("description"))
                        content { addResolvedContent(callbackBody?.maps("content").orEmpty()) }
                        if (callbackBody?.get("required") as? Boolean == true) { required(true) }
                    }
                    responses {
                        for (response in callback.maps("responses").sortedBy { it["status"] as String }) {
                            response(response["status"] as String) {
                                description(descriptionOf(response))
                                content { addResolvedContent(response.maps("content")) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ContentBuilder.addResolvedContent(contents: List<Map<String, Any?>>) {
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

    private fun generateOperationId(
        method: String,
        route: Map<String, Any?>,
        pathParamPrefix: String = "By"
    ): String =
        when (val operationId = route["operationId"] as String) {
            AUTO_GENERATE ->
                method.lowercase() + (route["path"] as String).split('/')
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

    private fun resolveMediaType(content: Map<String, Any?>): Pair<String, MediaTypeBuilder.() -> Unit>? {
        val from = content["from"] as? ClassDefinition
        val fromIsNull = from == null || from.fullName == nullClassName
        var type = content.text("type")
        var mimeType = (content["mimeType"] as? String)?.takeIf { it != ContentType.AUTODETECT }

        if (mimeType == null) {
            if (fromIsNull) {
                mimeType = type
                type = null
            } else {
                mimeType = detectContentType(from!!)
            }
        }

        if (mimeType == null) {
            return null
        }

        val resolvedType = type
        val format = content.text("format")
        val properties = content.maps("properties").takeIf { it.isNotEmpty() }
        val additionalProperties = content.child("additionalProperties")?.takeIf { it["_ignored"] != true }

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

    private fun ExampleHolder.applyExample(content: Map<String, Any?>) {
        content.text("example")?.let { example(it) }
        content.maps("exampleObjects").takeIf { it.isNotEmpty() }?.let { applyExamples(it) }
    }

    private fun ObjectSchemaBuilder.buildProperties(properties: List<Map<String, Any?>>) {
        for (property in properties) {
            val propertyFormat = property.text("format")
            val from = property["from"] as? ClassDefinition
            val isResolved = from != null && from.fullName != nullClassName

            if (property["isArray"] as? Boolean == true) {
                if (isResolved) {
                    arrayProperty(property["name"] as String, createTypeDescriptionWithReferences(from!!))
                } else {
                    arrayProperty(property["name"] as String, property["type"] as String, propertyFormat)
                }
            } else {
                if (isResolved) {
                    property(property["name"] as String, createTypeDescriptionWithReferences(from!!))
                } else {
                    property(property["name"] as String, property["type"] as String, propertyFormat)
                }
            }
        }
    }

    private fun ObjectSchemaBuilder.buildAdditionalProperties(additionalProperties: Map<String, Any?>) {
        val from = additionalProperties["from"] as? ClassDefinition

        if (from != null && from.fullName != nullClassName) {
            additionalProperties(createTypeDescriptionWithReferences(from))
        } else {
            additionalProperties(additionalProperties.text("type"), additionalProperties.text("format"))
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

    private fun descriptionOf(response: Map<String, Any?>): String =
        response.text("description")
            ?: defaultStatusDescription(response["status"] as String)
            ?: ""

}

private fun Map<String, Any?>.formattedPath(): String =
    (this["path"] as String).let { if (it.startsWith("/")) it else "/$it" }

private fun Map<String, Any?>.text(key: String): String? =
    (this[key] as? String)?.takeIf { it != NULL_STRING }

private fun Map<String, Any?>.exampleText(): String? =
    (this["example"] as? String)?.takeIf { it.isNotEmpty() }

private fun Map<String, Any?>.texts(key: String): List<String> =
    (this[key] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

private fun Map<String, Any?>.child(key: String): Map<String, Any?>? {
    @Suppress("UNCHECKED_CAST")
    return this[key] as? Map<String, Any?>
}

private fun Map<String, Any?>.maps(key: String): List<Map<String, Any?>> =
    (this[key] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
