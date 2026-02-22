package io.javalin.openapi.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.openapi.ApiKeyAuth
import io.javalin.openapi.BasicAuth
import io.javalin.openapi.BearerAuth
import io.javalin.openapi.CookieAuth
import io.javalin.openapi.OAuth2
import io.javalin.openapi.OpenApiExampleProperty
import io.javalin.openapi.OpenApiInfo
import io.javalin.openapi.OpenApiServer
import io.javalin.openapi.OpenID
import io.javalin.openapi.Security
import io.javalin.openapi.SecurityScheme
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.processor.generators.ExampleGenerator
import io.javalin.openapi.experimental.processor.generators.ResultScheme
import io.javalin.openapi.experimental.processor.generators.toExampleProperty
import io.javalin.openapi.experimental.processor.shared.createArrayNode
import io.javalin.openapi.experimental.processor.shared.createObjectNode
import io.javalin.openapi.experimental.processor.shared.jsonMapper
import java.util.TreeMap
import java.util.function.Consumer

fun interface ComponentSchemaResolver {
    fun resolve(type: ClassDefinition): ResultScheme
}

class OpenApiSchemaBuilder {
    private val root = createObjectNode()
    private val paths = createObjectNode()
    private val componentSchemas = createObjectNode()
    internal val componentReferences = mutableMapOf<String, ClassDefinition>()

    private val refCollector: (Set<ClassDefinition>) -> Unit = { refs ->
        componentReferences.putAll(refs.associateBy { it.fullName })
    }

    fun openApiVersion(version: String): OpenApiSchemaBuilder = apply {
        root.put("openapi", version)
    }

    fun info(configure: Consumer<OpenApiInfo>): OpenApiSchemaBuilder = apply {
        val infoJson = jsonMapper.convertValue(OpenApiInfo().also { configure.accept(it) }, JsonNode::class.java)
        val existingInfo = root.get("info")
        val updatedInfo: JsonNode =
            if (existingInfo != null) {
                jsonMapper.readerForUpdating(existingInfo).readValue(infoJson)
            } else {
                infoJson
            }
        root.set<JsonNode>("info", updatedInfo)
    }

    fun server(configure: Consumer<OpenApiServer>): OpenApiSchemaBuilder = apply {
        val serversArray = root.get("servers") as? ArrayNode ?: createArrayNode()
        serversArray.add(jsonMapper.convertValue(OpenApiServer().also { configure.accept(it) }, JsonNode::class.java))
        root.set<JsonNode>("servers", serversArray)
    }

    /** Add a named security scheme */
    fun withSecurityScheme(name: String, scheme: SecurityScheme): OpenApiSchemaBuilder = apply {
        val components = root.get("components") as? ObjectNode ?: createObjectNode().also { root.set<JsonNode>("components", it) }
        val schemes = components.get("securitySchemes") as? ObjectNode ?: createObjectNode().also { components.set<JsonNode>("securitySchemes", it) }
        schemes.set<JsonNode>(name, jsonMapper.convertValue(scheme, JsonNode::class.java))
    }

    /** Add HTTP Basic authentication scheme */
    @JvmOverloads
    fun withBasicAuth(name: String = "BasicAuth", configure: Consumer<BasicAuth> = Consumer {}): OpenApiSchemaBuilder =
        withSecurityScheme(name, BasicAuth().also { configure.accept(it) })

    /** Add HTTP Bearer authentication scheme */
    @JvmOverloads
    fun withBearerAuth(name: String = "BearerAuth", configure: Consumer<BearerAuth> = Consumer {}): OpenApiSchemaBuilder =
        withSecurityScheme(name, BearerAuth().also { configure.accept(it) })

    /** Add API Key authentication scheme */
    @JvmOverloads
    fun withApiKeyAuth(name: String = "ApiKeyAuth", apiKeyName: String = "X-API-Key", configure: Consumer<ApiKeyAuth> = Consumer {}): OpenApiSchemaBuilder =
        withSecurityScheme(name, ApiKeyAuth(name = apiKeyName).also { configure.accept(it) })

    /** Add Cookie authentication scheme */
    @JvmOverloads
    fun withCookieAuth(name: String = "CookieAuth", sessionCookie: String = "JSESSIONID", configure: Consumer<CookieAuth> = Consumer {}): OpenApiSchemaBuilder =
        withSecurityScheme(name, CookieAuth(name = sessionCookie).also { configure.accept(it) })

    /** Add OpenID Connect authentication scheme */
    @JvmOverloads
    fun withOpenID(name: String, openIdConnectUrl: String, configure: Consumer<OpenID> = Consumer {}): OpenApiSchemaBuilder =
        withSecurityScheme(name, OpenID(openIdConnectUrl = openIdConnectUrl).also { configure.accept(it) })

    /** Add OAuth2 authentication scheme */
    @JvmOverloads
    fun withOAuth2(name: String, description: String, configure: Consumer<OAuth2> = Consumer {}): OpenApiSchemaBuilder =
        withSecurityScheme(name, OAuth2(description = description).also { configure.accept(it) })

    /** Add a global security requirement */
    @JvmOverloads
    fun withGlobalSecurity(name: String, configure: Consumer<Security> = Consumer {}): OpenApiSchemaBuilder = apply {
        val security = Security(name = name).also { configure.accept(it) }
        val securityArray = root.get("security") as? ArrayNode ?: createArrayNode().also { root.set<JsonNode>("security", it) }
        val entry = createObjectNode()
        val scopesArray = createArrayNode()
        security.scopes.forEach { scopesArray.add(it) }
        entry.set<JsonNode>(security.name, scopesArray)
        securityArray.add(entry)
    }

    fun path(path: String): PathItemBuilder {
        if (!paths.has(path)) {
            paths.set<JsonNode>(path, createObjectNode())
        }
        return PathItemBuilder(paths.get(path) as ObjectNode, refCollector)
    }

    fun addComponentSchema(name: String, schema: ResultScheme) {
        refCollector(schema.references)
        componentSchemas.set<JsonNode>(name, schema.json)
    }

    fun hasComponentSchema(name: String): Boolean =
        componentSchemas.has(name)

    fun resolveComponentReferences(resolver: ComponentSchemaResolver) {
        val maxIterations = 1000
        val generatedComponents = TreeMap<String, Pair<ClassDefinition, ObjectNode>?> { a, b -> a.compareTo(b) }
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
                componentReferences.putAll(references.associateBy { it.fullName })
                generatedComponents[name] = componentReference to json
            }
        }

        componentReferences.clear()

        generatedComponents
            .mapNotNull { it.value }
            .filter { (type, _) -> !hasComponentSchema(type.simpleName) }
            .forEach { (type, json) ->
                componentSchemas.set<JsonNode>(type.simpleName, json)
            }
    }

    private fun buildRoot(): ObjectNode {
        root.set<JsonNode>("paths", paths)
        val components = root.get("components") as? ObjectNode ?: createObjectNode()
        components.set<JsonNode>("schemas", componentSchemas)
        root.set<JsonNode>("components", components)
        return root
    }

    fun toJson(): String = buildRoot().toPrettyString()

    fun toCompactJson(): String = buildRoot().toString()

    companion object {
        @JvmStatic
        fun fromJson(json: String): OpenApiSchemaBuilder {
            val builder = OpenApiSchemaBuilder()
            val parsed = jsonMapper.readTree(json) as ObjectNode

            parsed.properties().forEach { (key, value) ->
                when (key) {
                    "paths" -> {
                        val pathsNode = value as? ObjectNode ?: return@forEach
                        pathsNode.properties().forEach { (pathKey, pathValue) ->
                            builder.paths.set<JsonNode>(pathKey, pathValue.deepCopy())
                        }
                    }
                    "components" -> {
                        val componentsNode = value as? ObjectNode ?: return@forEach
                        val schemas = componentsNode.get("schemas") as? ObjectNode
                        schemas?.properties()?.forEach { (schemaName, schemaValue) ->
                            builder.componentSchemas.set<JsonNode>(schemaName, schemaValue.deepCopy())
                        }
                        // Preserve non-schema component entries (securitySchemes, etc.)
                        componentsNode.properties()
                            .filter { it.key != "schemas" }
                            .forEach { (compKey, compValue) ->
                                val rootComponents = builder.root.get("components") as? ObjectNode
                                    ?: createObjectNode().also { builder.root.set<JsonNode>("components", it) }
                                rootComponents.set<JsonNode>(compKey, compValue.deepCopy())
                            }
                    }
                    else -> builder.root.set<JsonNode>(key, value.deepCopy())
                }
            }

            return builder
        }
    }
}

@DslMarker
annotation class OpenApiSchemaDsl

@OpenApiSchemaDsl
class SchemaBuilder {
    private val schema = createObjectNode()

    fun type(type: String): SchemaBuilder = apply { schema.put("type", type) }
    fun format(format: String): SchemaBuilder = apply { schema.put("format", format) }
    fun ref(ref: String): SchemaBuilder = apply { schema.put("\$ref", ref) }

    internal fun build(): ObjectNode = schema
}

class PathItemBuilder(
    private val pathItem: ObjectNode,
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
) {

    fun operation(method: String, configure: OperationBuilder.() -> Unit) {
        val existing = pathItem.get(method) as? ObjectNode
        val builder = OperationBuilder(refCollector, existing)
        builder.configure()
        pathItem.set<JsonNode>(method, builder.build())
    }

    fun operation(method: String, configure: Consumer<OperationBuilder>) = operation(method) { configure.accept(this) }
}

class OperationBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
    existing: ObjectNode? = null,
) {

    private val operation = createObjectNode().also { op ->
        existing?.properties()?.forEach { (key, value) ->
            if (key !in MANAGED_FIELDS) op.set<JsonNode>(key, value.deepCopy())
        }
    }
    private var tagsArray = (existing?.get("tags") as? ArrayNode)?.deepCopy() ?: createArrayNode()
    private var parametersArray = (existing?.get("parameters") as? ArrayNode)?.deepCopy() ?: createArrayNode()
    private var responsesObject = (existing?.get("responses") as? ObjectNode)?.deepCopy()
    private var callbacksObject = (existing?.get("callbacks") as? ObjectNode)?.deepCopy()
    private var securityArray = (existing?.get("security") as? ArrayNode)?.deepCopy() ?: createArrayNode()
    private var requestBodyObject = (existing?.get("requestBody") as? ObjectNode)?.deepCopy()
    private var deprecatedValue = existing?.get("deprecated")?.asBoolean()

    companion object {
        private val MANAGED_FIELDS = setOf("tags", "parameters", "requestBody", "responses", "callbacks", "security", "deprecated")
    }

    fun tags(vararg tags: String) {
        tagsArray = createArrayNode()
        tags.forEach { tagsArray.add(it) }
    }

    fun tags(tags: Collection<String>) {
        tagsArray = createArrayNode()
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
        deprecatedValue = value
    }

    fun addTag(tag: String) {
        tagsArray.add(tag)
    }

    fun addTags(tags: Collection<String>) {
        tags.forEach { tagsArray.add(it) }
    }

    fun parameters(configure: ParametersBuilder.() -> Unit) {
        val builder = ParametersBuilder(refCollector, parametersArray)
        builder.configure()
        parametersArray = builder.build()
    }

    fun requestBody(configure: RequestBodyBuilder.() -> Unit) {
        val builder = RequestBodyBuilder(refCollector, requestBodyObject)
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            requestBodyObject = built
        }
    }

    fun responses(configure: ResponsesBuilder.() -> Unit) {
        val builder = ResponsesBuilder(refCollector, responsesObject)
        builder.configure()
        responsesObject = builder.build()
    }

    fun callbacks(configure: CallbacksBuilder.() -> Unit) {
        val builder = CallbacksBuilder(refCollector, callbacksObject)
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            callbacksObject = built
        }
    }

    fun security(configure: SecurityBuilder.() -> Unit) {
        val builder = SecurityBuilder(securityArray)
        builder.configure()
        securityArray = builder.build()
    }

    fun parameters(configure: Consumer<ParametersBuilder>) = parameters { configure.accept(this) }
    fun requestBody(configure: Consumer<RequestBodyBuilder>) = requestBody { configure.accept(this) }
    fun responses(configure: Consumer<ResponsesBuilder>) = responses { configure.accept(this) }
    fun callbacks(configure: Consumer<CallbacksBuilder>) = callbacks { configure.accept(this) }
    fun security(configure: Consumer<SecurityBuilder>) = security { configure.accept(this) }

    internal fun build(): ObjectNode {
        val result = createObjectNode()

        if (tagsArray.size() > 0) {
            result.set<JsonNode>("tags", tagsArray)
        }

        // Copy properties set directly on operation (summary, description, operationId)
        for (entry in operation.properties()) {
            result.set<JsonNode>(entry.key, entry.value)
        }

        if (parametersArray.size() > 0) {
            result.set<JsonNode>("parameters", parametersArray)
        }

        requestBodyObject?.let { result.set<JsonNode>("requestBody", it) }

        result.set<JsonNode>("responses", responsesObject ?: createObjectNode())

        callbacksObject?.let { result.set<JsonNode>("callbacks", it) }

        deprecatedValue?.let { result.put("deprecated", it) }

        if (securityArray.size() > 0) {
            result.set<JsonNode>("security", securityArray)
        }

        return result
    }
}

@OpenApiSchemaDsl
class ParametersBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
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
        param.set<JsonNode>("schema", schemaJson)
        if (example != null) {
            param.put("example", example)
        }

        // Replace existing parameter with same name+in, or append
        val existingIndex = (0 until parameters.size()).firstOrNull { i ->
            val existing = parameters.get(i) as? ObjectNode
            existing?.get("name")?.asText() == name && existing?.get("in")?.asText() == location
        }
        if (existingIndex != null) {
            parameters.set(existingIndex, param)
        } else {
            parameters.add(param)
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
            schema = ResultScheme(SchemaBuilder().apply(schema).build(), emptySet()),
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
    ) = parameter(name, location, description, required, deprecated, allowEmptyValue, example) { schema.accept(this) }

    internal fun build(): ArrayNode = parameters
}

class RequestBodyBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
    existing: ObjectNode? = null,
) {

    private val requestBody = createObjectNode().also { rb ->
        existing?.get("description")?.let { rb.set<JsonNode>("description", it.deepCopy()) }
        existing?.get("required")?.let { rb.set<JsonNode>("required", it.deepCopy()) }
    }
    private var contentObject = (existing?.get("content") as? ObjectNode)?.deepCopy()

    fun description(value: String?) {
        value?.let { requestBody.put("description", it) }
    }

    fun required(value: Boolean) {
        requestBody.put("required", value)
    }

    fun content(configure: ContentBuilder.() -> Unit) {
        val builder = ContentBuilder(refCollector, contentObject)
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            contentObject = built
        }
    }

    fun content(configure: Consumer<ContentBuilder>) = content { configure.accept(this) }

    internal fun build(): ObjectNode {
        val result = createObjectNode()

        // description first
        if (requestBody.has("description")) {
            result.set<JsonNode>("description", requestBody.get("description"))
        }

        // then content
        contentObject?.let { result.set<JsonNode>("content", it) }

        // If no content and no description, return empty (will be skipped by caller)
        if (result.size() == 0) {
            return result
        }

        // required is only added when there's already description or content
        if (requestBody.has("required")) {
            result.set<JsonNode>("required", requestBody.get("required"))
        }

        return result
    }
}

class ContentBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
    existing: ObjectNode? = null,
) {

    private val content = existing ?: createObjectNode()

    fun mediaType(mimeType: String, configure: MediaTypeBuilder.() -> Unit) {
        val existingMediaType = content.get(mimeType) as? ObjectNode
        val builder = MediaTypeBuilder(refCollector, existingMediaType)
        builder.configure()
        content.set<JsonNode>(mimeType, builder.build())
    }

    fun mediaType(mimeType: String, configure: Consumer<MediaTypeBuilder>) = mediaType(mimeType) { configure.accept(this) }

    internal fun build(): ObjectNode = content
}

interface ExampleHolder {
    fun example(value: String)
    fun exampleJson(value: JsonNode)

    fun applyExamples(exampleObjects: List<OpenApiExampleProperty>) {
        val generatorResult = ExampleGenerator.generateFromExamples(exampleObjects.map { it.toExampleProperty() })
        when {
            generatorResult.simpleValue != null -> example(generatorResult.simpleValue!!)
            generatorResult.jsonElement != null -> exampleJson(generatorResult.jsonElement!!)
        }
    }
}

@OpenApiSchemaDsl
class MediaTypeBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
    existing: ObjectNode? = null,
) : ExampleHolder {

    private val mediaType = createObjectNode().also { mt ->
        existing?.get("example")?.let { mt.set<JsonNode>("example", it.deepCopy()) }
    }
    private var schemaObject = (existing?.get("schema") as? ObjectNode)?.deepCopy()

    fun schema(resolved: ResultScheme) {
        refCollector(resolved.references)
        schemaObject = resolved.json
    }

    fun schema(configure: SchemaBuilder.() -> Unit) {
        schemaObject = SchemaBuilder().apply(configure).build()
    }

    fun schema(configure: Consumer<SchemaBuilder>) = schema { configure.accept(this) }

    fun objectSchema(configure: ObjectSchemaBuilder.() -> Unit) {
        val builder = ObjectSchemaBuilder(refCollector)
        builder.configure()
        schemaObject = builder.build()
    }

    fun objectSchema(configure: Consumer<ObjectSchemaBuilder>) = objectSchema { configure.accept(this) }

    override fun example(value: String) {
        mediaType.put("example", value)
    }

    override fun exampleJson(value: JsonNode) {
        mediaType.set<JsonNode>("example", value)
    }

    internal fun build(): ObjectNode {
        val result = createObjectNode()

        schemaObject?.let { schema ->
            if (schema.size() > 0) {
                result.set<JsonNode>("schema", schema)
            }
        }

        for (entry in mediaType.properties()) {
            result.set<JsonNode>(entry.key, entry.value)
        }

        return result
    }
}

@OpenApiSchemaDsl
class ObjectSchemaBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
) : ExampleHolder {

    private val properties = createObjectNode()
    private var additionalPropertiesObject: ObjectNode? = null
    private var exampleValue: String? = null
    private var exampleJsonValue: JsonNode? = null

    fun property(name: String, schema: ResultScheme) {
        refCollector(schema.references)
        properties.set<JsonNode>(name, schema.json)
    }

    fun property(name: String, schema: SchemaBuilder.() -> Unit) {
        properties.set<JsonNode>(name, SchemaBuilder().apply(schema).build())
    }

    fun property(name: String, schema: Consumer<SchemaBuilder>) = property(name) { schema.accept(this) }

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

    fun arrayProperty(name: String, items: Consumer<SchemaBuilder>) = arrayProperty(name) { items.accept(this) }

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
        additionalPropertiesObject = schema.json
    }

    fun additionalProperties(schema: SchemaBuilder.() -> Unit) {
        additionalPropertiesObject = SchemaBuilder().apply(schema).build()
    }

    fun additionalProperties(schema: Consumer<SchemaBuilder>) = additionalProperties { schema.accept(this) }

    fun additionalProperties(type: String?, format: String?) {
        val schema = createObjectNode()
        type?.let { schema.put("type", it) }
        format?.let { schema.put("format", it) }
        additionalPropertiesObject = schema
    }

    override fun example(value: String) {
        exampleValue = value
        exampleJsonValue = null
    }

    override fun exampleJson(value: JsonNode) {
        exampleJsonValue = value
        exampleValue = null
    }

    internal fun build(): ObjectNode {
        val result = createObjectNode()
        result.put("type", "object")

        if (properties.size() > 0) {
            result.set<JsonNode>("properties", properties)
        }

        additionalPropertiesObject?.let { result.set<JsonNode>("additionalProperties", it) }

        when {
            exampleValue != null -> result.put("example", exampleValue)
            exampleJsonValue != null -> result.set<JsonNode>("example", exampleJsonValue)
        }

        return result
    }
}

class ResponsesBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
    existing: ObjectNode? = null,
) {

    private val responses = existing ?: createObjectNode()

    fun response(status: String, configure: ResponseBuilder.() -> Unit) {
        val existingResponse = responses.get(status) as? ObjectNode
        val builder = ResponseBuilder(refCollector, existingResponse)
        builder.configure()
        responses.set<JsonNode>(status, builder.build())
    }

    fun response(status: String, configure: Consumer<ResponseBuilder>) = response(status) { configure.accept(this) }

    internal fun build(): ObjectNode = responses
}

class ResponseBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
    existing: ObjectNode? = null,
) {

    private val response = createObjectNode().also { r ->
        existing?.get("description")?.let { r.set<JsonNode>("description", it.deepCopy()) }
    }
    private var contentObject = (existing?.get("content") as? ObjectNode)?.deepCopy()
    private var headersObject = (existing?.get("headers") as? ObjectNode)?.deepCopy()

    fun description(value: String?) {
        value?.let { response.put("description", it) }
    }

    fun content(configure: ContentBuilder.() -> Unit) {
        val builder = ContentBuilder(refCollector, contentObject)
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            contentObject = built
        }
    }

    fun headers(configure: HeadersBuilder.() -> Unit) {
        val builder = HeadersBuilder(refCollector, headersObject)
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            headersObject = built
        }
    }

    fun content(configure: Consumer<ContentBuilder>) = content { configure.accept(this) }
    fun headers(configure: Consumer<HeadersBuilder>) = headers { configure.accept(this) }

    internal fun build(): ObjectNode {
        val result = createObjectNode()

        if (response.has("description")) {
            result.set<JsonNode>("description", response.get("description"))
        }

        contentObject?.let { result.set<JsonNode>("content", it) }
        headersObject?.let { result.set<JsonNode>("headers", it) }

        return result
    }
}

@OpenApiSchemaDsl
class HeadersBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
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
            schema = ResultScheme(SchemaBuilder().apply(schema).build(), emptySet()),
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
    ) = header(name, description, required, deprecated, allowEmptyValue, example) { schema.accept(this) }

    internal fun build(): ObjectNode = headers
}

class CallbacksBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
    existing: ObjectNode? = null,
) {

    private val callbacks = existing ?: createObjectNode()

    fun callback(name: String, url: String, method: String, configure: CallbackOperationBuilder.() -> Unit) {
        val eventObject = if (callbacks.has(name)) {
            callbacks.get(name) as ObjectNode
        } else {
            createObjectNode().also { callbacks.set<JsonNode>(name, it) }
        }

        val urlObject = if (eventObject.has(url)) {
            eventObject.get(url) as ObjectNode
        } else {
            createObjectNode().also { eventObject.set<JsonNode>(url, it) }
        }

        val existingOp = urlObject.get(method) as? ObjectNode
        val builder = CallbackOperationBuilder(refCollector, existingOp)
        builder.configure()
        urlObject.set<JsonNode>(method, builder.build())
    }

    fun callback(name: String, url: String, method: String, configure: Consumer<CallbackOperationBuilder>) = callback(name, url, method) { configure.accept(this) }

    internal fun build(): ObjectNode = callbacks
}

class CallbackOperationBuilder(
    private val refCollector: (Set<ClassDefinition>) -> Unit = {},
    existing: ObjectNode? = null,
) {

    private val operation = createObjectNode().also { op ->
        existing?.properties()?.forEach { (key, value) ->
            if (key !in MANAGED_FIELDS) op.set<JsonNode>(key, value.deepCopy())
        }
    }
    private var requestBodyObject = (existing?.get("requestBody") as? ObjectNode)?.deepCopy()
    private var responsesObject = (existing?.get("responses") as? ObjectNode)?.deepCopy()

    companion object {
        private val MANAGED_FIELDS = setOf("requestBody", "responses")
    }

    fun summary(value: String?) {
        value?.let { operation.put("summary", it) }
    }

    fun description(value: String?) {
        value?.let { operation.put("description", it) }
    }

    fun requestBody(configure: RequestBodyBuilder.() -> Unit) {
        val builder = RequestBodyBuilder(refCollector, requestBodyObject)
        builder.configure()
        val built = builder.build()
        if (built.size() > 0) {
            requestBodyObject = built
        }
    }

    fun responses(configure: ResponsesBuilder.() -> Unit) {
        val builder = ResponsesBuilder(refCollector, responsesObject)
        builder.configure()
        responsesObject = builder.build()
    }

    fun requestBody(configure: Consumer<RequestBodyBuilder>) = requestBody { configure.accept(this) }
    fun responses(configure: Consumer<ResponsesBuilder>) = responses { configure.accept(this) }

    internal fun build(): ObjectNode {
        val result = createObjectNode()

        for (entry in operation.properties()) {
            result.set<JsonNode>(entry.key, entry.value)
        }

        requestBodyObject?.let { result.set<JsonNode>("requestBody", it) }
        responsesObject?.let { result.set<JsonNode>("responses", it) }

        return result
    }
}

class SecurityBuilder(existing: ArrayNode? = null) {

    private val security = existing ?: createArrayNode()

    fun securityRequirement(name: String, vararg scopes: String) {
        val entry = createObjectNode()
        val scopesArray = createArrayNode()
        scopes.forEach { scopesArray.add(it) }
        entry.set<JsonNode>(name, scopesArray)
        security.add(entry)
    }

    internal fun build(): ArrayNode = security
}
