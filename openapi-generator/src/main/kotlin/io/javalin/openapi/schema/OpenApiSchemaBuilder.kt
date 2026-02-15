package io.javalin.openapi.schema

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.javalin.openapi.experimental.processor.shared.toPrettyString

@JvmInline
value class ResolvedSchema(internal val json: JsonObject)

class OpenApiSchemaBuilder {

    private val root = JsonObject()
    private val paths = JsonObject()
    private val componentSchemas = JsonObject()

    fun openApiVersion(version: String): OpenApiSchemaBuilder = apply {
        root.addProperty("openapi", version)
    }

    fun info(title: String?, version: String?): OpenApiSchemaBuilder = apply {
        val info = JsonObject()
        info.addProperty("title", title ?: "")
        info.addProperty("version", version ?: "")
        root.add("info", info)
    }

    fun path(path: String): PathItemBuilder {
        if (!paths.has(path)) {
            paths.add(path, JsonObject())
        }
        return PathItemBuilder(paths.getAsJsonObject(path))
    }

    fun addComponentSchema(name: String, schema: JsonObject) {
        componentSchemas.add(name, schema)
    }

    fun hasComponentSchema(name: String): Boolean =
        componentSchemas.has(name)

    fun toJson(): String {
        root.add("paths", paths)
        val components = JsonObject()
        components.add("schemas", componentSchemas)
        root.add("components", components)
        return root.toPrettyString()
    }
}

class PathItemBuilder(private val pathItem: JsonObject) {

    fun operation(method: String, configure: OperationBuilder.() -> Unit) {
        val builder = OperationBuilder()
        builder.configure()
        pathItem.add(method, builder.build())
    }
}

class OperationBuilder {

    private val operation = JsonObject()
    private var tagsArray = JsonArray()
    private var parametersArray = JsonArray()
    private var responsesObject: JsonObject? = null
    private var callbacksObject: JsonObject? = null
    private var securityArray = JsonArray()
    private var requestBodyObject: JsonObject? = null

    fun tags(vararg tags: String) {
        tagsArray = JsonArray()
        tags.forEach { tagsArray.add(it) }
    }

    fun tags(tags: Collection<String>) {
        tagsArray = JsonArray()
        tags.forEach { tagsArray.add(it) }
    }

    fun summary(value: String?) {
        value?.let { operation.addProperty("summary", it) }
    }

    fun description(value: String?) {
        value?.let { operation.addProperty("description", it) }
    }

    fun operationId(value: String?) {
        value?.let { operation.addProperty("operationId", it) }
    }

    fun deprecated(value: Boolean) {
        operation.addProperty("deprecated", value)
    }

    fun parameters(configure: ParametersBuilder.() -> Unit) {
        val builder = ParametersBuilder()
        builder.configure()
        parametersArray = builder.build()
    }

    fun requestBody(configure: RequestBodyBuilder.() -> Unit) {
        val builder = RequestBodyBuilder()
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            requestBodyObject = built
        }
    }

    fun responses(configure: ResponsesBuilder.() -> Unit) {
        val builder = ResponsesBuilder()
        builder.configure()
        responsesObject = builder.build()
    }

    fun callbacks(configure: CallbacksBuilder.() -> Unit) {
        val builder = CallbacksBuilder()
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            callbacksObject = built
        }
    }

    fun security(configure: SecurityBuilder.() -> Unit) {
        val builder = SecurityBuilder()
        builder.configure()
        securityArray = builder.build()
    }

    internal fun build(): JsonObject {
        val result = JsonObject()

        result.add("tags", tagsArray)

        // Copy properties set directly on operation (summary, description, operationId)
        for (entry in operation.entrySet()) {
            if (entry.key != "deprecated") {
                result.add(entry.key, entry.value)
            }
        }

        result.add("parameters", parametersArray)

        requestBodyObject?.let { result.add("requestBody", it) }

        result.add("responses", responsesObject ?: JsonObject())

        callbacksObject?.let { result.add("callbacks", it) }

        result.addProperty("deprecated", operation.get("deprecated")?.asBoolean ?: false)

        result.add("security", securityArray)

        return result
    }
}

class ParametersBuilder {

    private val parameters = JsonArray()

    fun parameter(
        name: String,
        location: String,
        schema: JsonObject,
        description: String? = null,
        required: Boolean = false,
        deprecated: Boolean = false,
        allowEmptyValue: Boolean = false,
        example: String? = null,
    ) {
        val param = JsonObject()
        param.addProperty("name", name)
        param.addProperty("in", location)
        description?.let { param.addProperty("description", it) }
        param.addProperty("required", required)
        param.addProperty("deprecated", deprecated)
        param.addProperty("allowEmptyValue", allowEmptyValue)
        if (example != null) {
            schema.addProperty("example", example)
        }
        param.add("schema", schema)
        parameters.add(param)
    }

    internal fun build(): JsonArray = parameters
}

class RequestBodyBuilder {

    private val requestBody = JsonObject()
    private var contentObject: JsonObject? = null

    fun description(value: String?) {
        value?.let { requestBody.addProperty("description", it) }
    }

    fun required(value: Boolean) {
        requestBody.addProperty("required", value)
    }

    fun content(configure: ContentBuilder.() -> Unit) {
        val builder = ContentBuilder()
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            contentObject = built
        }
    }

    internal fun build(): JsonObject {
        val result = JsonObject()

        // description first
        if (requestBody.has("description")) {
            result.add("description", requestBody.get("description"))
        }

        // then content
        contentObject?.let { result.add("content", it) }

        // If no content and no description, return empty (will be skipped by caller)
        if (result.size() == 0) {
            return result
        }

        // required is only added when there's already description or content
        if (requestBody.has("required")) {
            result.add("required", requestBody.get("required"))
        }

        return result
    }
}

class ContentBuilder {

    private val content = JsonObject()

    fun mediaType(
        mimeType: String,
        schema: JsonObject? = null,
        example: String? = null,
        exampleJson: JsonElement? = null,
    ) {
        val mediaType = JsonObject()

        if (schema != null && schema.size() > 0) {
            mediaType.add("schema", schema)
        }

        when {
            example != null -> mediaType.addProperty("example", example)
            exampleJson != null -> mediaType.add("example", exampleJson)
        }

        content.add(mimeType, mediaType)
    }

    fun mediaType(mimeType: String, configure: MediaTypeBuilder.() -> Unit) {
        val builder = MediaTypeBuilder()
        builder.configure()
        content.add(mimeType, builder.build())
    }

    internal fun build(): JsonObject = content
}

interface ExampleHolder {
    fun example(value: String)
    fun exampleJson(value: JsonElement)
}

class MediaTypeBuilder : ExampleHolder {

    private val mediaType = JsonObject()
    private var schemaObject: JsonObject? = null

    fun schema(resolved: JsonObject) {
        schemaObject = resolved
    }

    fun simpleSchema(type: String?, format: String?) {
        val schema = JsonObject()
        type?.let { schema.addProperty("type", it) }
        format?.let { schema.addProperty("format", it) }
        schemaObject = schema
    }

    fun objectSchema(configure: ObjectSchemaBuilder.() -> Unit) {
        val builder = ObjectSchemaBuilder()
        builder.configure()
        schemaObject = builder.build()
    }

    override fun example(value: String) {
        mediaType.addProperty("example", value)
    }

    override fun exampleJson(value: JsonElement) {
        mediaType.add("example", value)
    }

    internal fun build(): JsonObject {
        val result = JsonObject()

        schemaObject?.let { schema ->
            if (schema.size() > 0) {
                result.add("schema", schema)
            }
        }

        for (entry in mediaType.entrySet()) {
            result.add(entry.key, entry.value)
        }

        return result
    }
}

class ObjectSchemaBuilder : ExampleHolder {

    private val properties = JsonObject()
    private var additionalPropertiesObject: JsonObject? = null
    private var exampleValue: String? = null
    private var exampleJsonValue: JsonElement? = null

    fun property(name: String, schema: JsonObject) {
        properties.add(name, schema)
    }

    fun property(name: String, type: String, format: String?) {
        val schema = JsonObject()
        schema.addProperty("type", type)
        format?.let { schema.addProperty("format", it) }
        properties.add(name, schema)
    }

    fun arrayProperty(name: String, itemSchema: JsonObject) {
        val schema = JsonObject()
        schema.addProperty("type", "array")
        schema.add("items", itemSchema)
        properties.add(name, schema)
    }

    fun arrayProperty(name: String, itemType: String, itemFormat: String?) {
        val itemSchema = JsonObject()
        itemSchema.addProperty("type", itemType)
        itemFormat?.let { itemSchema.addProperty("format", it) }
        val schema = JsonObject()
        schema.addProperty("type", "array")
        schema.add("items", itemSchema)
        properties.add(name, schema)
    }

    fun additionalProperties(schema: JsonObject) {
        additionalPropertiesObject = schema
    }

    fun additionalProperties(type: String?, format: String?) {
        val schema = JsonObject()
        type?.let { schema.addProperty("type", it) }
        format?.let { schema.addProperty("format", it) }
        additionalPropertiesObject = schema
    }

    override fun example(value: String) {
        exampleValue = value
        exampleJsonValue = null
    }

    override fun exampleJson(value: JsonElement) {
        exampleJsonValue = value
        exampleValue = null
    }

    internal fun build(): JsonObject {
        val result = JsonObject()
        result.addProperty("type", "object")

        if (properties.size() > 0) {
            result.add("properties", properties)
        }

        additionalPropertiesObject?.let { result.add("additionalProperties", it) }

        when {
            exampleValue != null -> result.addProperty("example", exampleValue)
            exampleJsonValue != null -> result.add("example", exampleJsonValue)
        }

        return result
    }
}

class ResponsesBuilder {

    private val responses = JsonObject()

    fun response(status: String, configure: ResponseBuilder.() -> Unit) {
        val builder = ResponseBuilder()
        builder.configure()
        responses.add(status, builder.build())
    }

    internal fun build(): JsonObject = responses
}

class ResponseBuilder {

    private val response = JsonObject()
    private var contentObject: JsonObject? = null
    private var headersObject: JsonObject? = null

    fun description(value: String?) {
        value?.let { response.addProperty("description", it) }
    }

    fun content(configure: ContentBuilder.() -> Unit) {
        val builder = ContentBuilder()
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            contentObject = built
        }
    }

    fun headers(configure: HeadersBuilder.() -> Unit) {
        val builder = HeadersBuilder()
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            headersObject = built
        }
    }

    internal fun build(): JsonObject {
        val result = JsonObject()

        if (response.has("description")) {
            result.add("description", response.get("description"))
        }

        contentObject?.let { result.add("content", it) }
        headersObject?.let { result.add("headers", it) }

        return result
    }
}

class HeadersBuilder {

    private val headers = JsonObject()

    fun header(
        name: String,
        schema: JsonObject,
        description: String? = null,
        required: Boolean = false,
        deprecated: Boolean = false,
        allowEmptyValue: Boolean = false,
        example: String? = null,
    ) {
        val header = JsonObject()
        description?.let { header.addProperty("description", it) }
        if (required) {
            header.addProperty("required", true)
        }
        if (deprecated) {
            header.addProperty("deprecated", true)
        }
        if (allowEmptyValue) {
            header.addProperty("allowEmptyValue", true)
        }
        if (example != null) {
            schema.addProperty("example", example)
        }
        header.add("schema", schema)
        headers.add(name, header)
    }

    internal fun build(): JsonObject = headers
}

class CallbacksBuilder {

    private val callbacks = JsonObject()

    fun callback(name: String, url: String, method: String, configure: CallbackOperationBuilder.() -> Unit) {
        val builder = CallbackOperationBuilder()
        builder.configure()

        val eventObject = if (callbacks.has(name)) {
            callbacks.getAsJsonObject(name)
        } else {
            JsonObject().also { callbacks.add(name, it) }
        }

        val urlObject = if (eventObject.has(url)) {
            eventObject.getAsJsonObject(url)
        } else {
            JsonObject().also { eventObject.add(url, it) }
        }

        urlObject.add(method, builder.build())
    }

    internal fun build(): JsonObject = callbacks
}

class CallbackOperationBuilder {

    private val operation = JsonObject()
    private var requestBodyObject: JsonObject? = null
    private var responsesObject: JsonObject? = null

    fun summary(value: String?) {
        value?.let { operation.addProperty("summary", it) }
    }

    fun description(value: String?) {
        value?.let { operation.addProperty("description", it) }
    }

    fun requestBody(configure: RequestBodyBuilder.() -> Unit) {
        val builder = RequestBodyBuilder()
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            requestBodyObject = built
        }
    }

    fun responses(configure: ResponsesBuilder.() -> Unit) {
        val builder = ResponsesBuilder()
        builder.configure()
        responsesObject = builder.build()
    }

    internal fun build(): JsonObject {
        val result = JsonObject()

        for (entry in operation.entrySet()) {
            result.add(entry.key, entry.value)
        }

        requestBodyObject?.let { result.add("requestBody", it) }
        responsesObject?.let { result.add("responses", it) }

        return result
    }
}

class SecurityBuilder {

    private val security = JsonArray()

    fun securityRequirement(name: String, vararg scopes: String) {
        val entry = JsonObject()
        val scopesArray = JsonArray()
        scopes.forEach { scopesArray.add(it) }
        entry.add(name, scopesArray)
        security.add(entry)
    }

    internal fun build(): JsonArray = security
}
