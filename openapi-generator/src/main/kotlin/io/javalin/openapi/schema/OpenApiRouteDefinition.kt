package io.javalin.openapi.schema

import io.javalin.introspection.ClassDefinition
import io.javalin.openapi.ContentType
import io.javalin.openapi.NULL_CLASS
import io.javalin.openapi.NULL_STRING
import io.javalin.openapi.experimental.processor.generators.ExampleProperty
import io.javalin.openapi.experimental.processor.generators.toExampleProperty

internal data class OpenApiRouteDefinition(
    val path: String,
    val methods: List<String>,
    val versions: List<String>,
    val ignore: Boolean,
    val summary: String?,
    val description: String?,
    val operationId: String,
    val deprecated: Boolean,
    val tags: List<String>,
    val cookies: List<OpenApiParameterDefinition>,
    val headers: List<OpenApiParameterDefinition>,
    val pathParameters: List<OpenApiParameterDefinition>,
    val queryParameters: List<OpenApiParameterDefinition>,
    val requestBody: OpenApiRequestBodyDefinition?,
    val callbacks: List<OpenApiCallbackDefinition>,
    val responses: List<OpenApiResponseDefinition>,
    val security: List<OpenApiSecurityDefinition>,
) {
    val formattedPath: String
        get() = when {
            path.startsWith("/") -> path
            else -> "/$path"
        }

    companion object {
        fun from(values: Map<String, Any?>): OpenApiRouteDefinition =
            OpenApiRouteDefinition(
                path = values.requiredString("path"),
                methods = values.strings("methods"),
                versions = values.strings("versions"),
                ignore = values.boolean("ignore"),
                summary = values.text("summary"),
                description = values.text("description"),
                operationId = values.requiredString("operationId"),
                deprecated = values.boolean("deprecated"),
                tags = values.strings("tags"),
                cookies = values.maps("cookies").map(OpenApiParameterDefinition::from),
                headers = values.maps("headers").map(OpenApiParameterDefinition::from),
                pathParameters = values.maps("pathParams").map(OpenApiParameterDefinition::from),
                queryParameters = values.maps("queryParams").map(OpenApiParameterDefinition::from),
                requestBody = values.child("requestBody")?.let(OpenApiRequestBodyDefinition::from),
                callbacks = values.maps("callbacks").map(OpenApiCallbackDefinition::from),
                responses = values.maps("responses").map(OpenApiResponseDefinition::from),
                security = values.maps("security").map(OpenApiSecurityDefinition::from),
            )
    }
}

internal data class OpenApiParameterDefinition(
    val name: String,
    val type: ClassDefinition,
    val description: String?,
    val required: Boolean,
    val deprecated: Boolean,
    val allowEmptyValue: Boolean,
    val example: String?,
) {
    companion object {
        fun from(values: Map<String, Any?>): OpenApiParameterDefinition =
            OpenApiParameterDefinition(
                name = values.requiredString("name"),
                type = values.requiredClassDefinition("type"),
                description = values.text("description"),
                required = values.boolean("required"),
                deprecated = values.boolean("deprecated"),
                allowEmptyValue = values.boolean("allowEmptyValue"),
                example = values.nonEmptyString("example"),
            )
    }
}

internal data class OpenApiRequestBodyDefinition(
    val content: List<OpenApiContentDefinition>,
    val required: Boolean,
    val description: String?,
) {
    companion object {
        fun from(values: Map<String, Any?>): OpenApiRequestBodyDefinition =
            OpenApiRequestBodyDefinition(
                content = values.maps("content").map(OpenApiContentDefinition::from),
                required = values.boolean("required"),
                description = values.text("description"),
            )
    }
}

internal data class OpenApiResponseDefinition(
    val status: String,
    val content: List<OpenApiContentDefinition>,
    val description: String?,
    val headers: List<OpenApiParameterDefinition>,
) {
    companion object {
        fun from(values: Map<String, Any?>): OpenApiResponseDefinition =
            OpenApiResponseDefinition(
                status = values.requiredString("status"),
                content = values.maps("content").map(OpenApiContentDefinition::from),
                description = values.text("description"),
                headers = values.maps("headers").map(OpenApiParameterDefinition::from),
            )
    }
}

internal data class OpenApiCallbackDefinition(
    val name: String,
    val url: String,
    val method: String,
    val summary: String?,
    val description: String?,
    val requestBody: OpenApiRequestBodyDefinition?,
    val responses: List<OpenApiResponseDefinition>,
) {
    companion object {
        fun from(values: Map<String, Any?>): OpenApiCallbackDefinition =
            OpenApiCallbackDefinition(
                name = values.requiredString("name"),
                url = values.requiredString("url"),
                method = values.requiredString("method"),
                summary = values.text("summary"),
                description = values.text("description"),
                requestBody = values.child("requestBody")?.let(OpenApiRequestBodyDefinition::from),
                responses = values.maps("responses").map(OpenApiResponseDefinition::from),
            )
    }
}

internal data class OpenApiSecurityDefinition(
    val name: String,
    val scopes: List<String>,
) {
    companion object {
        fun from(values: Map<String, Any?>): OpenApiSecurityDefinition =
            OpenApiSecurityDefinition(
                name = values.requiredString("name"),
                scopes = values.strings("scopes"),
            )
    }
}

internal data class OpenApiContentDefinition(
    val from: ClassDefinition?,
    val mimeType: String?,
    val type: String?,
    val format: String?,
    val properties: List<OpenApiContentPropertyDefinition>,
    val additionalProperties: OpenApiContentDefinition?,
    val example: String?,
    val exampleObjects: List<ExampleProperty>,
) {
    val resolvedSource: ClassDefinition?
        get() = from?.takeUnless { it.fullName == NULL_CLASS::class.java.name }

    companion object {
        fun from(values: Map<String, Any?>): OpenApiContentDefinition =
            OpenApiContentDefinition(
                from = values.classDefinition("from"),
                mimeType = values.string("mimeType")?.takeIf { it != ContentType.AUTODETECT },
                type = values.text("type"),
                format = values.text("format"),
                properties = values.maps("properties").map(OpenApiContentPropertyDefinition::from),
                additionalProperties = values.child("additionalProperties")
                    ?.takeIf { !it.boolean("_ignored") }
                    ?.let(::from),
                example = values.text("example"),
                exampleObjects = values.maps("exampleObjects").map { it.toExampleProperty() },
            )
    }
}

internal data class OpenApiContentPropertyDefinition(
    val from: ClassDefinition?,
    val name: String,
    val isArray: Boolean,
    val type: String?,
    val format: String?,
) {
    val resolvedSource: ClassDefinition?
        get() = from?.takeUnless { it.fullName == NULL_CLASS::class.java.name }

    companion object {
        fun from(values: Map<String, Any?>): OpenApiContentPropertyDefinition =
            OpenApiContentPropertyDefinition(
                from = values.classDefinition("from"),
                name = values.requiredString("name"),
                isArray = values.boolean("isArray"),
                type = values.text("type"),
                format = values.text("format"),
            )
    }
}

private fun Map<String, Any?>.requiredString(key: String): String = get(key) as String

private fun Map<String, Any?>.requiredClassDefinition(key: String): ClassDefinition = get(key) as ClassDefinition

private fun Map<String, Any?>.string(key: String): String? = get(key) as? String

private fun Map<String, Any?>.text(key: String): String? = string(key)?.takeIf { it != NULL_STRING }

private fun Map<String, Any?>.nonEmptyString(key: String): String? = string(key)?.takeIf { it.isNotEmpty() }

private fun Map<String, Any?>.boolean(key: String): Boolean = get(key) as? Boolean ?: false

private fun Map<String, Any?>.classDefinition(key: String): ClassDefinition? = get(key) as? ClassDefinition

private fun Map<String, Any?>.strings(key: String): List<String> =
    (get(key) as? List<*>)?.filterIsInstance<String>().orEmpty()

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.child(key: String): Map<String, Any?>? = get(key) as? Map<String, Any?>

private fun Map<String, Any?>.maps(key: String): List<Map<String, Any?>> =
    (get(key) as? List<*>)?.filterIsInstance<Map<String, Any?>>().orEmpty()
