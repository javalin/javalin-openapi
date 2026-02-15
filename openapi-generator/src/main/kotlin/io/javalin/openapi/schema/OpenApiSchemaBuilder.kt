package io.javalin.openapi.schema

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.javalin.openapi.OpenApiExampleProperty
import io.javalin.openapi.experimental.AnnotationProcessorContext
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.processor.generators.ExampleGenerator
import io.javalin.openapi.experimental.processor.generators.ResultScheme
import io.javalin.openapi.experimental.processor.generators.toExampleProperty
import io.javalin.openapi.experimental.processor.shared.info
import io.javalin.openapi.experimental.processor.shared.toPrettyString
import java.util.TreeMap

class OpenApiSchemaBuilder {

    private val root = JsonObject()
    private val paths = JsonObject()
    private val componentSchemas = JsonObject()
    internal val componentReferences = mutableMapOf<String, ClassDefinition>()

    private val refCollector: (Set<ClassDefinition>) -> Unit = { refs ->
        componentReferences.putAll(refs.associateBy { it.fullName })
    }

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
        return PathItemBuilder(paths.getAsJsonObject(path), refCollector)
    }

    fun addComponentSchema(name: String, schema: ResultScheme) {
        refCollector(schema.references)
        componentSchemas.add(name, schema.json)
    }

    fun hasComponentSchema(name: String): Boolean =
        componentSchemas.has(name)

    fun resolveComponentReferences(context: AnnotationProcessorContext) {
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
                val alreadyExists = hasComponentSchema(type.simpleName)

                context.inDebug {
                    if (alreadyExists) {
                        context.env.messager.info("Scheme component '${type.simpleName}' already exists. Generated scheme for ${type.fullName} won't be added to the OpenAPI document.")
                    }
                }

                !alreadyExists
            }
            .forEach { (type, json) ->
                componentSchemas.add(type.simpleName, json)
            }
    }

    fun toJson(): String {
        root.add("paths", paths)
        val components = JsonObject()
        components.add("schemas", componentSchemas)
        root.add("components", components)
        return root.toPrettyString()
    }
}

class PathItemBuilder(
    private val pathItem: JsonObject,
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
) {

    fun operation(method: String, configure: OperationBuilder.() -> Unit) {
        val builder = OperationBuilder(refCollector)
        builder.configure()
        pathItem.add(method, builder.build())
    }
}

class OperationBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
) {

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
        val builder = ParametersBuilder(refCollector)
        builder.configure()
        parametersArray = builder.build()
    }

    fun requestBody(configure: RequestBodyBuilder.() -> Unit) {
        val builder = RequestBodyBuilder(refCollector)
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            requestBodyObject = built
        }
    }

    fun responses(configure: ResponsesBuilder.() -> Unit) {
        val builder = ResponsesBuilder(refCollector)
        builder.configure()
        responsesObject = builder.build()
    }

    fun callbacks(configure: CallbacksBuilder.() -> Unit) {
        val builder = CallbacksBuilder(refCollector)
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

class ParametersBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
) {

    private val parameters = JsonArray()

    fun parameter(
        name: String,
        location: String,
        schema: ResultScheme,
        description: String? = null,
        required: Boolean = false,
        deprecated: Boolean = false,
        allowEmptyValue: Boolean = false,
        example: String? = null,
    ) {
        refCollector(schema.references)
        val schemaJson = schema.json
        val param = JsonObject()
        param.addProperty("name", name)
        param.addProperty("in", location)
        description?.let { param.addProperty("description", it) }
        param.addProperty("required", required)
        param.addProperty("deprecated", deprecated)
        param.addProperty("allowEmptyValue", allowEmptyValue)
        if (example != null) {
            schemaJson.addProperty("example", example)
        }
        param.add("schema", schemaJson)
        parameters.add(param)
    }

    internal fun build(): JsonArray = parameters
}

class RequestBodyBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
) {

    private val requestBody = JsonObject()
    private var contentObject: JsonObject? = null

    fun description(value: String?) {
        value?.let { requestBody.addProperty("description", it) }
    }

    fun required(value: Boolean) {
        requestBody.addProperty("required", value)
    }

    fun content(configure: ContentBuilder.() -> Unit) {
        val builder = ContentBuilder(refCollector)
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

class ContentBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
) {

    private val content = JsonObject()

    fun mediaType(
        mimeType: String,
        schema: ResultScheme? = null,
        example: String? = null,
        exampleJson: JsonElement? = null,
    ) {
        schema?.let { refCollector(it.references) }
        val mediaType = JsonObject()

        if (schema != null && schema.json.size() > 0) {
            mediaType.add("schema", schema.json)
        }

        when {
            example != null -> mediaType.addProperty("example", example)
            exampleJson != null -> mediaType.add("example", exampleJson)
        }

        content.add(mimeType, mediaType)
    }

    fun mediaType(mimeType: String, configure: MediaTypeBuilder.() -> Unit) {
        val builder = MediaTypeBuilder(refCollector)
        builder.configure()
        content.add(mimeType, builder.build())
    }

    internal fun build(): JsonObject = content
}

interface ExampleHolder {
    fun example(value: String)
    fun exampleJson(value: JsonElement)

    fun applyExamples(exampleObjects: List<OpenApiExampleProperty>) {
        val generatorResult = ExampleGenerator.generateFromExamples(exampleObjects.map { it.toExampleProperty() })
        when {
            generatorResult.simpleValue != null -> example(generatorResult.simpleValue!!)
            generatorResult.jsonElement != null -> exampleJson(generatorResult.jsonElement!!)
        }
    }
}

class MediaTypeBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
) : ExampleHolder {

    private val mediaType = JsonObject()
    private var schemaObject: JsonObject? = null

    fun schema(resolved: ResultScheme) {
        refCollector(resolved.references)
        schemaObject = resolved.json
    }

    fun simpleSchema(type: String?, format: String?) {
        val schema = JsonObject()
        type?.let { schema.addProperty("type", it) }
        format?.let { schema.addProperty("format", it) }
        schemaObject = schema
    }

    fun objectSchema(configure: ObjectSchemaBuilder.() -> Unit) {
        val builder = ObjectSchemaBuilder(refCollector)
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

class ObjectSchemaBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
) : ExampleHolder {

    private val properties = JsonObject()
    private var additionalPropertiesObject: JsonObject? = null
    private var exampleValue: String? = null
    private var exampleJsonValue: JsonElement? = null

    fun property(name: String, schema: ResultScheme) {
        refCollector(schema.references)
        properties.add(name, schema.json)
    }

    fun property(name: String, type: String, format: String?) {
        val schema = JsonObject()
        schema.addProperty("type", type)
        format?.let { schema.addProperty("format", it) }
        properties.add(name, schema)
    }

    fun arrayProperty(name: String, itemSchema: ResultScheme) {
        refCollector(itemSchema.references)
        val schema = JsonObject()
        schema.addProperty("type", "array")
        schema.add("items", itemSchema.json)
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

    fun additionalProperties(schema: ResultScheme) {
        refCollector(schema.references)
        additionalPropertiesObject = schema.json
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

class ResponsesBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
) {

    private val responses = JsonObject()

    fun response(status: String, configure: ResponseBuilder.() -> Unit) {
        val builder = ResponseBuilder(refCollector)
        builder.configure()
        responses.add(status, builder.build())
    }

    internal fun build(): JsonObject = responses
}

class ResponseBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
) {

    private val response = JsonObject()
    private var contentObject: JsonObject? = null
    private var headersObject: JsonObject? = null

    fun description(value: String?) {
        value?.let { response.addProperty("description", it) }
    }

    fun content(configure: ContentBuilder.() -> Unit) {
        val builder = ContentBuilder(refCollector)
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            contentObject = built
        }
    }

    fun headers(configure: HeadersBuilder.() -> Unit) {
        val builder = HeadersBuilder(refCollector)
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

class HeadersBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
) {

    private val headers = JsonObject()

    fun header(
        name: String,
        schema: ResultScheme,
        description: String? = null,
        required: Boolean = false,
        deprecated: Boolean = false,
        allowEmptyValue: Boolean = false,
        example: String? = null,
    ) {
        refCollector(schema.references)
        val schemaJson = schema.json
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
            schemaJson.addProperty("example", example)
        }
        header.add("schema", schemaJson)
        headers.add(name, header)
    }

    internal fun build(): JsonObject = headers
}

class CallbacksBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
) {

    private val callbacks = JsonObject()

    fun callback(name: String, url: String, method: String, configure: CallbackOperationBuilder.() -> Unit) {
        val builder = CallbackOperationBuilder(refCollector)
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

class CallbackOperationBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
) {

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
        val builder = RequestBodyBuilder(refCollector)
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            requestBodyObject = built
        }
    }

    fun responses(configure: ResponsesBuilder.() -> Unit) {
        val builder = ResponsesBuilder(refCollector)
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
