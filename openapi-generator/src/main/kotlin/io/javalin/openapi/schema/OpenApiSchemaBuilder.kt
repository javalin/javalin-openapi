package io.javalin.openapi.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.openapi.ApiKeyAuth
import io.javalin.openapi.BasicAuth
import io.javalin.openapi.BearerAuth
import io.javalin.openapi.CookieAuth
import io.javalin.openapi.OAuth2
import io.javalin.openapi.OpenApiInfo
import io.javalin.openapi.OpenApiServer
import io.javalin.openapi.OpenID
import io.javalin.openapi.Security
import io.javalin.openapi.SecurityScheme
import io.javalin.openapi.experimental.OpenApiType
import io.javalin.openapi.experimental.mergeExtraFrom
import io.javalin.openapi.experimental.processor.generators.ResultScheme
import io.javalin.openapi.experimental.processor.shared.createArrayNode
import io.javalin.openapi.experimental.processor.shared.createObjectNode
import io.javalin.openapi.experimental.processor.shared.jsonMapper
import java.util.TreeMap
import java.util.function.Consumer

fun interface ComponentSchemaResolver {
    fun resolve(type: OpenApiType): ResultScheme
}

class OpenApiSchemaBuilder {
    private val root = createObjectNode()
    private val paths = createObjectNode()
    private val componentSchemas = createObjectNode()
    internal val componentReferences = mutableMapOf<String, OpenApiType>()

    private val refCollector: (Set<OpenApiType>) -> Unit = { references ->
        references.forEach { reference -> mergeComponentReference(reference) }
    }

    fun openApiVersion(version: String): OpenApiSchemaBuilder =
        apply {
            root.put("openapi", version)
        }

    fun info(configure: Consumer<OpenApiInfo>): OpenApiSchemaBuilder =
        apply {
            val infoJson = jsonMapper.convertValue(OpenApiInfo().also { configure.accept(it) }, JsonNode::class.java)
            val existingInfo = root.get("info")
            val updatedInfo: JsonNode = when {
                existingInfo != null -> jsonMapper.readerForUpdating(existingInfo).readValue(infoJson)
                else -> infoJson
            }
            root.set<JsonNode>("info", updatedInfo)
        }

    fun ensureInfo(title: String = "", version: String = ""): OpenApiSchemaBuilder =
        apply {
            val info = root.get("info") as? ObjectNode
                ?: createObjectNode().also { root.set<JsonNode>("info", it) }
            if (!info.has("title")) {
                info.put("title", title)
            }
            if (!info.has("version")) {
                info.put("version", version)
            }
        }

    fun server(configure: Consumer<OpenApiServer>): OpenApiSchemaBuilder =
        apply {
            val serversArray = root.get("servers") as? ArrayNode ?: createArrayNode()
            serversArray.add(jsonMapper.convertValue(OpenApiServer().also { configure.accept(it) }, JsonNode::class.java))
            root.set<JsonNode>("servers", serversArray)
        }

    /** Add a named security scheme */
    fun withSecurityScheme(name: String, scheme: SecurityScheme): OpenApiSchemaBuilder =
        apply {
            val components = root.get("components") as? ObjectNode
                ?: createObjectNode().also { root.set<JsonNode>("components", it) }
            val schemes = components.get("securitySchemes") as? ObjectNode
                ?: createObjectNode().also { components.set<JsonNode>("securitySchemes", it) }
            schemes.set<JsonNode>(name, jsonMapper.convertValue(scheme, JsonNode::class.java))
        }

    /** Add HTTP Basic authentication scheme */
    @JvmOverloads
    fun withBasicAuth(
        name: String = "BasicAuth",
        configure: Consumer<BasicAuth> = Consumer {},
    ): OpenApiSchemaBuilder =
        withSecurityScheme(name, BasicAuth().also { configure.accept(it) })

    /** Add HTTP Bearer authentication scheme */
    @JvmOverloads
    fun withBearerAuth(
        name: String = "BearerAuth",
        configure: Consumer<BearerAuth> = Consumer {},
    ): OpenApiSchemaBuilder =
        withSecurityScheme(name, BearerAuth().also { configure.accept(it) })

    /** Add API Key authentication scheme */
    @JvmOverloads
    fun withApiKeyAuth(
        name: String = "ApiKeyAuth",
        apiKeyName: String = "X-API-Key",
        configure: Consumer<ApiKeyAuth> = Consumer {},
    ): OpenApiSchemaBuilder =
        withSecurityScheme(name, ApiKeyAuth(name = apiKeyName).also { configure.accept(it) })

    /** Add Cookie authentication scheme */
    @JvmOverloads
    fun withCookieAuth(
        name: String = "CookieAuth",
        sessionCookie: String = "JSESSIONID",
        configure: Consumer<CookieAuth> = Consumer {},
    ): OpenApiSchemaBuilder =
        withSecurityScheme(name, CookieAuth(name = sessionCookie).also { configure.accept(it) })

    /** Add OpenID Connect authentication scheme */
    @JvmOverloads
    fun withOpenID(
        name: String,
        openIdConnectUrl: String,
        configure: Consumer<OpenID> = Consumer {},
    ): OpenApiSchemaBuilder =
        withSecurityScheme(name, OpenID(openIdConnectUrl = openIdConnectUrl).also { configure.accept(it) })

    /** Add OAuth2 authentication scheme */
    @JvmOverloads
    fun withOAuth2(
        name: String,
        description: String,
        configure: Consumer<OAuth2> = Consumer {},
    ): OpenApiSchemaBuilder =
        withSecurityScheme(name, OAuth2(description = description).also { configure.accept(it) })

    /** Add a global security requirement */
    @JvmOverloads
    fun withGlobalSecurity(
        name: String,
        configure: Consumer<Security> = Consumer {},
    ): OpenApiSchemaBuilder =
        apply {
            val security = Security(name = name).also { configure.accept(it) }
            val securityArray = root.get("security") as? ArrayNode
                ?: createArrayNode().also { root.set<JsonNode>("security", it) }
            securityArray.addSecurityRequirement(security.name, security.scopes)
        }

    fun path(path: String): PathItemBuilder {
        val pathItem = paths.get(path) as? ObjectNode
            ?: createObjectNode().also { paths.set<JsonNode>(path, it) }
        return PathItemBuilder(pathItem = pathItem, refCollector = refCollector)
    }

    fun hasOperation(path: String, method: String): Boolean =
        (paths.get(path) as? ObjectNode)?.has(method) == true

    fun addComponentSchema(name: String, schema: ResultScheme) {
        refCollector(schema.references)
        componentSchemas.set<JsonNode>(name, schema.json)
    }

    fun hasComponentSchema(name: String): Boolean =
        componentSchemas.has(name)

    fun resolveComponentReferences(resolver: ComponentSchemaResolver) {
        val maxIterations = 1000
        val generatedComponents = TreeMap<String, Pair<OpenApiType, ObjectNode>?>()
        var iteration = 0

        while (generatedComponents.size < componentReferences.size) {
            if (++iteration > maxIterations) {
                val unresolved = componentReferences.keys - generatedComponents.keys
                throw IllegalStateException(
                    "Component reference resolution exceeded $maxIterations iterations. " +
                    "Possible unbounded type expansion. Unresolved: $unresolved"
                )
            }

            for ((name, componentReference) in componentReferences.toMutableMap()) {
                if (generatedComponents.containsKey(name)) {
                    continue
                }

                if (componentReference.fullName == "java.lang.Object") {
                    generatedComponents[name] = null
                    continue
                }

                val (json, references) = resolver.resolve(componentReference)
                references.forEach { reference ->
                    if (mergeComponentReference(reference)) {
                        generatedComponents.remove(reference.fullName)
                    }
                }
                generatedComponents[name] = componentReference to json
            }
        }

        componentReferences.clear()

        val simpleNameToFullName = mutableMapOf<String, String>()

        for ((_, component) in generatedComponents) {
            val (type, json) = component ?: continue
            val existing = simpleNameToFullName[type.simpleName]
            if (existing != null && existing != type.fullName) {
                throw IllegalStateException(
                    "Component schema name collision: '${type.simpleName}' maps to both '$existing' and '${type.fullName}'. " +
                    "Use @OpenApiName to provide a unique name for one of the conflicting types."
                )
            }
            simpleNameToFullName[type.simpleName] = type.fullName
            if (!hasComponentSchema(type.simpleName)) {
                componentSchemas.set<JsonNode>(type.simpleName, json)
            }
        }
    }

    private fun mergeComponentReference(reference: OpenApiType): Boolean =
        when (val existing = componentReferences[reference.fullName]) {
            null -> {
                componentReferences[reference.fullName] = reference
                false
            }
            else -> existing.mergeExtraFrom(reference)
        }

    private fun buildRoot(): ObjectNode {
        root.set<JsonNode>("paths", paths)
        val components = root.get("components") as? ObjectNode ?: createObjectNode()
        components.set<JsonNode>("schemas", componentSchemas)
        root.set<JsonNode>("components", components)
        return root
    }

    fun toJson(): String =
        buildRoot().toPrettyString()

    fun toCompactJson(): String =
        buildRoot().toString()

    companion object {
        @JvmStatic
        fun fromJson(json: String): OpenApiSchemaBuilder {
            val builder = OpenApiSchemaBuilder()
            val parsed = jsonMapper.readTree(json) as ObjectNode
            (parsed.remove("paths") as? ObjectNode)?.let { builder.paths.setAll<ObjectNode>(it) }
            val components = parsed.get("components") as? ObjectNode
            (components?.remove("schemas") as? ObjectNode)?.let { builder.componentSchemas.setAll<ObjectNode>(it) }
            if (components == null || components.isEmpty) {
                parsed.remove("components")
            }
            builder.root.setAll<ObjectNode>(parsed)
            return builder
        }
    }
}

@DslMarker
annotation class OpenApiSchemaDsl

@OpenApiSchemaDsl
class SchemaBuilder {
    private val schema = createObjectNode()

    fun type(type: String): SchemaBuilder =
        apply { schema.put("type", type) }
    fun format(format: String): SchemaBuilder =
        apply { schema.put("format", format) }
    fun ref(ref: String): SchemaBuilder =
        apply { schema.put($$"$ref", ref) }

    internal fun build(): ObjectNode = schema
}

@OpenApiSchemaDsl
class PathItemBuilder(
    private val pathItem: ObjectNode,
    private val refCollector: (Set<OpenApiType>) -> Unit = {},
) {

    fun operation(method: String, configure: OperationBuilder.() -> Unit) {
        val existing = pathItem.get(method) as? ObjectNode
        val builder = OperationBuilder(refCollector = refCollector, existing = existing)
        builder.configure()
        pathItem.set<JsonNode>(method, builder.build())
    }

    fun operation(method: String, configure: Consumer<OperationBuilder>) =
        operation(method) { configure.accept(this) }
}

@OpenApiSchemaDsl
class OperationBuilder(
    private val refCollector: (Set<OpenApiType>) -> Unit = {},
    existing: ObjectNode? = null,
) {

    private val operation = existing?.deepCopy() ?: createObjectNode()

    fun tags(vararg tags: String) {
        tags(tags.asList())
    }

    fun tags(tags: Collection<String>) {
        val tagsArray = operation.putArray("tags")
        tags.forEach { tagsArray.add(it) }
    }

    fun summary(value: String?) {
        value?.let { operation.put("summary", it) }
    }

    fun description(value: String?) {
        value?.let { operation.put("description", it) }
    }

    fun operationId(value: String?) {
        value?.let { operation.put("operationId", it) }
    }

    fun deprecated(value: Boolean) {
        operation.put("deprecated", value)
    }

    fun addTag(tag: String) =
        addTags(listOf(tag))

    fun addTags(tags: Collection<String>) {
        val tagsArray = operation.get("tags") as? ArrayNode ?: operation.putArray("tags")
        tags.forEach { tagsArray.add(it) }
    }

    fun parameters(configure: ParametersBuilder.() -> Unit) {
        val builder = ParametersBuilder(refCollector = refCollector, existing = operation.get("parameters") as? ArrayNode)
        builder.configure()
        operation.set<JsonNode>("parameters", builder.build())
    }

    fun requestBody(configure: RequestBodyBuilder.() -> Unit) {
        val builder = RequestBodyBuilder(refCollector = refCollector, existing = operation.get("requestBody") as? ObjectNode)
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            operation.set<JsonNode>("requestBody", built)
        }
    }

    fun responses(configure: ResponsesBuilder.() -> Unit) {
        val builder = ResponsesBuilder(refCollector = refCollector, existing = operation.get("responses") as? ObjectNode)
        builder.configure()
        operation.set<JsonNode>("responses", builder.build())
    }

    fun callbacks(configure: CallbacksBuilder.() -> Unit) {
        val builder = CallbacksBuilder(refCollector = refCollector, existing = operation.get("callbacks") as? ObjectNode)
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            operation.set<JsonNode>("callbacks", built)
        }
    }

    fun security(configure: SecurityBuilder.() -> Unit) {
        val builder = SecurityBuilder(operation.get("security") as? ArrayNode)
        builder.configure()
        operation.set<JsonNode>("security", builder.build())
    }

    fun parameters(configure: Consumer<ParametersBuilder>) =
        parameters { configure.accept(this) }
    fun requestBody(configure: Consumer<RequestBodyBuilder>) =
        requestBody { configure.accept(this) }
    fun responses(configure: Consumer<ResponsesBuilder>) =
        responses { configure.accept(this) }
    fun callbacks(configure: Consumer<CallbacksBuilder>) =
        callbacks { configure.accept(this) }
    fun security(configure: Consumer<SecurityBuilder>) =
        security { configure.accept(this) }

    internal fun build(): ObjectNode {
        val result = createObjectNode()

        (operation.get("tags") as? ArrayNode)
            ?.takeIf { !it.isEmpty }
            ?.let { result.set<JsonNode>("tags", it) }

        val managedFields = setOf("tags", "parameters", "requestBody", "responses", "callbacks", "security", "deprecated")
        for ((key, value) in operation.properties()) {
            if (key !in managedFields) {
                result.set<JsonNode>(key, value)
            }
        }

        (operation.get("parameters") as? ArrayNode)
            ?.takeIf { !it.isEmpty }
            ?.let { result.set<JsonNode>("parameters", it) }

        (operation.get("requestBody") as? ObjectNode)?.let { result.set<JsonNode>("requestBody", it) }
        result.set<JsonNode>("responses", operation.get("responses") as? ObjectNode ?: createObjectNode())
        (operation.get("callbacks") as? ObjectNode)?.let { result.set<JsonNode>("callbacks", it) }
        if (operation.get("deprecated")?.asBoolean() == true) {
            result.put("deprecated", true)
        }

        (operation.get("security") as? ArrayNode)
            ?.takeIf { !it.isEmpty }
            ?.let { result.set<JsonNode>("security", it) }

        return result
    }
}

@OpenApiSchemaDsl
class ParametersBuilder(
    private val refCollector: (Set<OpenApiType>) -> Unit = {},
    existing: ArrayNode? = null,
) {

    private val parameters = existing ?: createArrayNode()

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
        val param = createObjectNode()
        param.put("name", name)
        param.put("in", location)
        description?.let { param.put("description", it) }
        if (required) param.put("required", true)
        if (deprecated) param.put("deprecated", true)
        if (allowEmptyValue) param.put("allowEmptyValue", true)
        when {
            schema.references.isNotEmpty() -> {
                val mediaTypeNode = createObjectNode()
                mediaTypeNode.set<JsonNode>("schema", schemaJson)
                val contentNode = createObjectNode()
                contentNode.set<JsonNode>("application/json", mediaTypeNode)
                param.set<JsonNode>("content", contentNode)
            }
            else -> param.set<JsonNode>("schema", schemaJson)
        }
        if (example != null) {
            param.put("example", example)
        }

        val existingIndex = (0 until parameters.size()).firstOrNull { i ->
            val existing = parameters.get(i) as? ObjectNode
            existing?.get("name")?.asText() == name && existing?.get("in")?.asText() == location
        }
        when {
            existingIndex != null -> parameters.set(existingIndex, param)
            else -> parameters.add(param)
        }
    }

    fun parameter(
        name: String,
        location: String,
        description: String? = null,
        required: Boolean = false,
        deprecated: Boolean = false,
        allowEmptyValue: Boolean = false,
        example: String? = null,
        schema: SchemaBuilder.() -> Unit,
    ) {
        parameter(
            name = name,
            location = location,
            schema = ResultScheme(
                json = SchemaBuilder().apply(schema).build(),
                references = emptySet(),
            ),
            description = description,
            required = required,
            deprecated = deprecated,
            allowEmptyValue = allowEmptyValue,
            example = example,
        )
    }

    fun parameter(
        name: String,
        location: String,
        description: String?,
        required: Boolean,
        deprecated: Boolean,
        allowEmptyValue: Boolean,
        example: String?,
        schema: Consumer<SchemaBuilder>,
    ) =
        parameter(
            name = name,
            location = location,
            description = description,
            required = required,
            deprecated = deprecated,
            allowEmptyValue = allowEmptyValue,
            example = example,
        ) { schema.accept(this) }

    internal fun build(): ArrayNode = parameters
}

@OpenApiSchemaDsl
class RequestBodyBuilder(
    private val refCollector: (Set<OpenApiType>) -> Unit = {},
    existing: ObjectNode? = null,
) {

    private val requestBody = existing?.deepCopy() ?: createObjectNode()

    fun description(value: String?) {
        value?.let { requestBody.put("description", it) }
    }

    fun required(value: Boolean) {
        requestBody.put("required", value)
    }

    fun content(configure: ContentBuilder.() -> Unit) {
        val builder = ContentBuilder(refCollector = refCollector, existing = requestBody.get("content") as? ObjectNode)
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            requestBody.set<JsonNode>("content", built)
        }
    }

    fun content(configure: Consumer<ContentBuilder>) =
        content { configure.accept(this) }

    internal fun build(): ObjectNode {
        val result = createObjectNode()

        if (requestBody.has("description")) {
            result.set<JsonNode>("description", requestBody.get("description"))
        }

        (requestBody.get("content") as? ObjectNode)?.let { result.set<JsonNode>("content", it) }

        if (result.size() == 0) {
            return result
        }

        if (requestBody.has("required")) {
            result.set<JsonNode>("required", requestBody.get("required"))
        }

        return result
    }
}

@OpenApiSchemaDsl
class ContentBuilder(
    private val refCollector: (Set<OpenApiType>) -> Unit = {},
    existing: ObjectNode? = null,
) {

    private val content = existing ?: createObjectNode()

    fun mediaType(mimeType: String, configure: MediaTypeBuilder.() -> Unit) {
        val existingMediaType = content.get(mimeType) as? ObjectNode
        val builder = MediaTypeBuilder(refCollector = refCollector, existing = existingMediaType)
        builder.configure()
        content.set<JsonNode>(mimeType, builder.build())
    }

    fun mediaType(mimeType: String, configure: Consumer<MediaTypeBuilder>) =
        mediaType(mimeType) { configure.accept(this) }

    internal fun build(): ObjectNode = content
}

interface ExampleHolder {
    fun example(value: String)
    fun exampleJson(value: JsonNode)
}

@OpenApiSchemaDsl
class MediaTypeBuilder(
    private val refCollector: (Set<OpenApiType>) -> Unit = {},
    existing: ObjectNode? = null,
) : ExampleHolder {

    private val mediaType = existing?.deepCopy() ?: createObjectNode()

    fun schema(resolved: ResultScheme) {
        refCollector(resolved.references)
        mediaType.set<JsonNode>("schema", resolved.json)
    }

    fun schema(configure: SchemaBuilder.() -> Unit) {
        mediaType.set<JsonNode>("schema", SchemaBuilder().apply(configure).build())
    }

    fun schema(configure: Consumer<SchemaBuilder>) =
        schema { configure.accept(this) }

    fun objectSchema(configure: ObjectSchemaBuilder.() -> Unit) {
        val builder = ObjectSchemaBuilder(refCollector)
        builder.configure()
        mediaType.set<JsonNode>("schema", builder.build())
    }

    fun objectSchema(configure: Consumer<ObjectSchemaBuilder>) =
        objectSchema { configure.accept(this) }

    override fun example(value: String) {
        mediaType.put("example", value)
    }

    override fun exampleJson(value: JsonNode) {
        mediaType.set<JsonNode>("example", value)
    }

    internal fun build(): ObjectNode {
        val result = createObjectNode()

        (mediaType.get("schema") as? ObjectNode)?.let { schema ->
            if (schema.size() > 0) {
                result.set<JsonNode>("schema", schema)
            }
        }

        mediaType.get("example")?.let { result.set<JsonNode>("example", it) }

        return result
    }
}

@OpenApiSchemaDsl
class ObjectSchemaBuilder(
    private val refCollector: (Set<OpenApiType>) -> Unit = {},
) : ExampleHolder {

    private val schema = createObjectNode()
    private val properties = schema.putObject("properties")

    fun property(name: String, schema: ResultScheme) {
        refCollector(schema.references)
        properties.set<JsonNode>(name, schema.json)
    }

    fun property(name: String, schema: SchemaBuilder.() -> Unit) {
        properties.set<JsonNode>(name, SchemaBuilder().apply(schema).build())
    }

    fun property(name: String, schema: Consumer<SchemaBuilder>) =
        property(name) { schema.accept(this) }

    fun property(name: String, type: String, format: String?) {
        val schema = createObjectNode()
        schema.put("type", type)
        format?.let { schema.put("format", it) }
        properties.set<JsonNode>(name, schema)
    }

    fun arrayProperty(name: String, itemSchema: ResultScheme) {
        refCollector(itemSchema.references)
        val schema = createObjectNode()
        schema.put("type", "array")
        schema.set<JsonNode>("items", itemSchema.json)
        properties.set<JsonNode>(name, schema)
    }

    fun arrayProperty(name: String, items: SchemaBuilder.() -> Unit) {
        val schema = createObjectNode()
        schema.put("type", "array")
        schema.set<JsonNode>("items", SchemaBuilder().apply(items).build())
        properties.set<JsonNode>(name, schema)
    }

    fun arrayProperty(name: String, items: Consumer<SchemaBuilder>) =
        arrayProperty(name) { items.accept(this) }

    fun arrayProperty(name: String, itemType: String, itemFormat: String?) {
        val itemSchema = createObjectNode()
        itemSchema.put("type", itemType)
        itemFormat?.let { itemSchema.put("format", it) }
        val schema = createObjectNode()
        schema.put("type", "array")
        schema.set<JsonNode>("items", itemSchema)
        properties.set<JsonNode>(name, schema)
    }

    fun additionalProperties(schema: ResultScheme) {
        refCollector(schema.references)
        this.schema.set<JsonNode>("additionalProperties", schema.json)
    }

    fun additionalProperties(schema: SchemaBuilder.() -> Unit) {
        this.schema.set<JsonNode>("additionalProperties", SchemaBuilder().apply(schema).build())
    }

    fun additionalProperties(schema: Consumer<SchemaBuilder>) =
        additionalProperties { schema.accept(this) }

    fun additionalProperties(type: String?, format: String?) {
        val schema = createObjectNode()
        type?.let { schema.put("type", it) }
        format?.let { schema.put("format", it) }
        this.schema.set<JsonNode>("additionalProperties", schema)
    }

    override fun example(value: String) {
        schema.put("example", value)
    }

    override fun exampleJson(value: JsonNode) {
        schema.set<JsonNode>("example", value)
    }

    internal fun build(): ObjectNode {
        val result = createObjectNode()
        result.put("type", "object")

        if (properties.size() > 0) {
            result.set<JsonNode>("properties", properties)
        }

        schema.get("additionalProperties")?.let { result.set<JsonNode>("additionalProperties", it) }
        schema.get("example")?.let { result.set<JsonNode>("example", it) }

        return result
    }
}

@OpenApiSchemaDsl
class ResponsesBuilder(
    private val refCollector: (Set<OpenApiType>) -> Unit = {},
    existing: ObjectNode? = null,
) {

    private val responses = existing ?: createObjectNode()

    fun response(status: String, configure: ResponseBuilder.() -> Unit) {
        val existingResponse = responses.get(status) as? ObjectNode
        val builder = ResponseBuilder(refCollector = refCollector, existing = existingResponse)
        builder.configure()
        responses.set<JsonNode>(status, builder.build())
    }

    fun response(status: String, configure: Consumer<ResponseBuilder>) =
        response(status) { configure.accept(this) }

    internal fun build(): ObjectNode = responses
}

@OpenApiSchemaDsl
class ResponseBuilder(
    private val refCollector: (Set<OpenApiType>) -> Unit = {},
    existing: ObjectNode? = null,
) {

    private val response = existing?.deepCopy() ?: createObjectNode()

    fun description(value: String?) {
        value?.let { response.put("description", it) }
    }

    fun content(configure: ContentBuilder.() -> Unit) {
        val builder = ContentBuilder(refCollector = refCollector, existing = response.get("content") as? ObjectNode)
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            response.set<JsonNode>("content", built)
        }
    }

    fun headers(configure: HeadersBuilder.() -> Unit) {
        val builder = HeadersBuilder(refCollector = refCollector, existing = response.get("headers") as? ObjectNode)
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            response.set<JsonNode>("headers", built)
        }
    }

    fun content(configure: Consumer<ContentBuilder>) =
        content { configure.accept(this) }
    fun headers(configure: Consumer<HeadersBuilder>) =
        headers { configure.accept(this) }

    internal fun build(): ObjectNode {
        val result = createObjectNode()
        result.put("description", response.get("description")?.asText() ?: "")

        (response.get("content") as? ObjectNode)?.let { result.set<JsonNode>("content", it) }
        (response.get("headers") as? ObjectNode)?.let { result.set<JsonNode>("headers", it) }

        return result
    }
}

@OpenApiSchemaDsl
class HeadersBuilder(
    private val refCollector: (Set<OpenApiType>) -> Unit = {},
    existing: ObjectNode? = null,
) {

    private val headers = existing ?: createObjectNode()

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
        val header = createObjectNode()
        description?.let { header.put("description", it) }
        if (required) {
            header.put("required", true)
        }
        if (deprecated) {
            header.put("deprecated", true)
        }
        if (allowEmptyValue) {
            header.put("allowEmptyValue", true)
        }
        header.set<JsonNode>("schema", schemaJson)
        if (example != null) {
            header.put("example", example)
        }
        headers.set<JsonNode>(name, header)
    }

    fun header(
        name: String,
        description: String? = null,
        required: Boolean = false,
        deprecated: Boolean = false,
        allowEmptyValue: Boolean = false,
        example: String? = null,
        schema: SchemaBuilder.() -> Unit,
    ) {
        header(
            name = name,
            schema = ResultScheme(
                json = SchemaBuilder().apply(schema).build(),
                references = emptySet(),
            ),
            description = description,
            required = required,
            deprecated = deprecated,
            allowEmptyValue = allowEmptyValue,
            example = example,
        )
    }

    fun header(
        name: String,
        description: String?,
        required: Boolean,
        deprecated: Boolean,
        allowEmptyValue: Boolean,
        example: String?,
        schema: Consumer<SchemaBuilder>,
    ) =
        header(
            name = name,
            description = description,
            required = required,
            deprecated = deprecated,
            allowEmptyValue = allowEmptyValue,
            example = example,
        ) { schema.accept(this) }

    internal fun build(): ObjectNode = headers
}

@OpenApiSchemaDsl
class CallbacksBuilder(
    private val refCollector: (Set<OpenApiType>) -> Unit = {},
    existing: ObjectNode? = null,
) {

    private val callbacks = existing ?: createObjectNode()

    fun callback(name: String, url: String, method: String, configure: CallbackOperationBuilder.() -> Unit) {
        val eventObject = when {
            callbacks.has(name) -> callbacks.get(name) as ObjectNode
            else -> createObjectNode().also { callbacks.set<JsonNode>(name, it) }
        }

        val urlObject = when {
            eventObject.has(url) -> eventObject.get(url) as ObjectNode
            else -> createObjectNode().also { eventObject.set<JsonNode>(url, it) }
        }

        val existingOp = urlObject.get(method) as? ObjectNode
        val builder = CallbackOperationBuilder(refCollector = refCollector, existing = existingOp)
        builder.configure()
        urlObject.set<JsonNode>(method, builder.build())
    }

    fun callback(name: String, url: String, method: String, configure: Consumer<CallbackOperationBuilder>) =
        callback(name = name, url = url, method = method) { configure.accept(this) }

    internal fun build(): ObjectNode = callbacks
}

@OpenApiSchemaDsl
class CallbackOperationBuilder(
    private val refCollector: (Set<OpenApiType>) -> Unit = {},
    existing: ObjectNode? = null,
) {

    private val operation = existing?.deepCopy() ?: createObjectNode()

    fun summary(value: String?) {
        value?.let { operation.put("summary", it) }
    }

    fun description(value: String?) {
        value?.let { operation.put("description", it) }
    }

    fun requestBody(configure: RequestBodyBuilder.() -> Unit) {
        val builder = RequestBodyBuilder(refCollector = refCollector, existing = operation.get("requestBody") as? ObjectNode)
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            operation.set<JsonNode>("requestBody", built)
        }
    }

    fun responses(configure: ResponsesBuilder.() -> Unit) {
        val builder = ResponsesBuilder(refCollector = refCollector, existing = operation.get("responses") as? ObjectNode)
        builder.configure()
        operation.set<JsonNode>("responses", builder.build())
    }

    fun requestBody(configure: Consumer<RequestBodyBuilder>) =
        requestBody { configure.accept(this) }
    fun responses(configure: Consumer<ResponsesBuilder>) =
        responses { configure.accept(this) }

    internal fun build(): ObjectNode {
        val result = createObjectNode()

        for ((key, value) in operation.properties()) {
            if (key != "requestBody" && key != "responses") {
                result.set<JsonNode>(key, value)
            }
        }

        (operation.get("requestBody") as? ObjectNode)?.let { result.set<JsonNode>("requestBody", it) }
        (operation.get("responses") as? ObjectNode)?.let { result.set<JsonNode>("responses", it) }

        return result
    }
}

@OpenApiSchemaDsl
class SecurityBuilder(existing: ArrayNode? = null) {

    private val security = existing ?: createArrayNode()

    fun securityRequirement(name: String, vararg scopes: String) {
        security.addSecurityRequirement(name, scopes.asList())
    }

    internal fun build(): ArrayNode = security
}

private fun ArrayNode.addSecurityRequirement(name: String, scopes: Iterable<String>) {
    val entry = createObjectNode()
    val scopesArray = createArrayNode()
    scopes.forEach { scopesArray.add(it) }
    entry.set<JsonNode>(name, scopesArray)
    add(entry)
}
