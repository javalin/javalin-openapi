package io.javalin.openapi.schema

import com.fasterxml.jackson.databind.JsonNode
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.processor.generators.ResultScheme
import io.javalin.openapi.experimental.processor.shared.createArrayNode
import io.javalin.openapi.experimental.processor.shared.createObjectNode
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class OpenApiSchemaBuilderTest {

    private fun builder(): OpenApiSchemaBuilder =
        OpenApiSchemaBuilder()
            .openApiVersion("3.1.0")
            .info { it.title("").version("") }

    @Nested
    inner class DocumentStructure {

        @Test
        fun `should build minimal document`() {
            val json = OpenApiSchemaBuilder()
                .openApiVersion("3.1.0")
                .info { it.title("My API").version("1.0") }
                .toJson()

            assertThatJson(json)
                .isObject
                .containsEntry("openapi", "3.1.0")
                .containsEntry("info", json("""{ "title": "My API", "version": "1.0" }"""))
                .containsEntry("paths", json("{}"))
                .containsEntry("components", json("""{ "schemas": {} }"""))
        }

        @Test
        fun `should build info with partial fields`() {
            val json = OpenApiSchemaBuilder()
                .openApiVersion("3.1.0")
                .info { it.title("API") }
                .toJson()

            assertThatJson(json).inPath("$.info").isObject
                .containsEntry("title", "API")
        }

        @Test
        fun `should merge info across multiple calls`() {
            val json = OpenApiSchemaBuilder()
                .openApiVersion("3.1.0")
                .info { it.title("Original Title").version("1.0") }
                .info { it.title("Updated Title").description("API Description") }
                .toJson()

            assertThatJson(json).inPath("$.info").isObject
                .containsEntry("title", "Updated Title")
                .containsEntry("version", "1.0")
                .containsEntry("description", "API Description")
        }

        @Test
        fun `should add servers`() {
            val json = builder()
                .server { it.url("https://api.example.com").description("Production") }
                .server { it.url("https://staging.example.com") }
                .toJson()

            assertThatJson(json).inPath("$.servers").isArray.hasSize(2)
            assertThatJson(json).inPath("$.servers[0]").isObject
                .containsEntry("url", "https://api.example.com")
                .containsEntry("description", "Production")
        }

        @Test
        fun `should produce compact json`() {
            val compact = OpenApiSchemaBuilder()
                .openApiVersion("3.1.0")
                .info { it.title("API").version("1.0") }
                .toCompactJson()

            assert(!compact.contains("\n"))
            assertThatJson(compact).isObject.containsEntry("openapi", "3.1.0")
        }
    }

    @Nested
    inner class SecurityConfiguration {

        @Test
        fun `should add security schemes via convenience methods`() {
            val json = builder()
                .withBasicAuth()
                .withBearerAuth()
                .toJson()

            assertThatJson(json).inPath("$.components.securitySchemes.BasicAuth").isObject
                .containsEntry("type", "http")
                .containsEntry("scheme", "basic")
            assertThatJson(json).inPath("$.components.securitySchemes.BearerAuth").isObject
                .containsEntry("type", "http")
                .containsEntry("scheme", "bearer")
        }

        @Test
        fun `should add API key auth with defaults`() {
            val json = builder().withApiKeyAuth().toJson()

            assertThatJson(json).inPath("$.components.securitySchemes.ApiKeyAuth").isObject
                .containsEntry("type", "apiKey")
                .containsEntry("name", "X-API-Key")
                .containsEntry("in", "header")
        }

        @Test
        fun `should add cookie auth with defaults`() {
            val json = builder().withCookieAuth().toJson()

            assertThatJson(json).inPath("$.components.securitySchemes.CookieAuth").isObject
                .containsEntry("type", "apiKey")
                .containsEntry("name", "JSESSIONID")
                .containsEntry("in", "cookie")
        }

        @Test
        fun `should add OpenID auth`() {
            val json = builder().withOpenID("OpenID", "https://example.com/.well-known/openid-configuration").toJson()

            assertThatJson(json).inPath("$.components.securitySchemes.OpenID").isObject
                .containsEntry("type", "openIdConnect")
                .containsEntry("openIdConnectUrl", "https://example.com/.well-known/openid-configuration")
        }

        @Test
        fun `should add OAuth2 auth`() {
            val json = builder().withOAuth2("OAuth2", "OAuth2 authentication").toJson()

            assertThatJson(json).inPath("$.components.securitySchemes.OAuth2").isObject
                .containsEntry("type", "oauth2")
                .containsEntry("description", "OAuth2 authentication")
        }

        @Test
        fun `should add global security`() {
            val json = builder()
                .withGlobalSecurity("BearerAuth")
                .withGlobalSecurity("OAuth2") { it.withScope("read").withScope("write") }
                .toJson()

            assertThatJson(json).inPath("$.security").isArray.hasSize(2)
            assertThatJson(json).inPath("$.security[0]").isObject.containsEntry("BearerAuth", json("[]"))
            assertThatJson(json).inPath("$.security[1]").isObject.containsEntry("OAuth2", json("""["read", "write"]"""))
        }

        @Test
        fun `should build operation-level security requirements`() {
            val schema = builder()
            schema.path("/secure").operation("get") {
                security {
                    securityRequirement("BearerAuth")
                    securityRequirement("OAuth2", "read", "write")
                }
            }

            val json = schema.toJson()

            assertThatJson(json).inPath("$.paths['/secure'].get.security").isArray.hasSize(2)
            assertThatJson(json).inPath("$.paths['/secure'].get.security[0]").isObject.containsEntry("BearerAuth", json("[]"))
            assertThatJson(json).inPath("$.paths['/secure'].get.security[1]").isObject.containsEntry("OAuth2", json("""["read", "write"]"""))
        }
    }

    @Nested
    inner class Operations {

        @Test
        fun `should build path with operation`() {
            val schema = builder()
            schema.path("/users").operation("get") {
                tags("users", "admin")
                summary("Get all users")
                description("Returns a list of users")
                operationId("getUsers")
                deprecated(false)
            }

            val json = schema.toJson()

            assertThatJson(json).inPath("$.paths['/users'].get").isObject
                .containsEntry("tags", json("""["users", "admin"]"""))
                .containsEntry("summary", "Get all users")
                .containsEntry("description", "Returns a list of users")
                .containsEntry("operationId", "getUsers")
                .containsEntry("deprecated", false)
                .containsEntry("responses", json("{}"))
                .doesNotContainKey("parameters")
                .doesNotContainKey("security")
        }

        @Test
        fun `should omit null summary and description`() {
            val schema = builder()
            schema.path("/test").operation("get") {
                summary(null)
                description(null)
                operationId(null)
            }

            assertThatJson(schema.toJson()).inPath("$.paths['/test'].get").isObject
                .doesNotContainKey("summary")
                .doesNotContainKey("description")
                .doesNotContainKey("operationId")
        }

        @Test
        fun `should preserve field ordering in operation`() {
            val schema = builder()
            schema.path("/test").operation("get") {
                tags("test")
                summary("Test")
                description("Desc")
                operationId("testOp")
                deprecated(true)
                security { securityRequirement("auth") }
            }

            assertThatJson(schema.toJson()).inPath("$.paths['/test'].get").isObject
                .containsKeys("tags", "summary", "description", "operationId", "responses", "deprecated", "security")
        }

        @Test
        fun `should support multiple methods on same path`() {
            val schema = builder()
            val path = schema.path("/users")
            path.operation("get") { summary("List users") }
            path.operation("post") { summary("Create user") }

            val json = schema.toJson()

            assertThatJson(json).inPath("$.paths['/users'].get.summary").isEqualTo("List users")
            assertThatJson(json).inPath("$.paths['/users'].post.summary").isEqualTo("Create user")
        }
    }

    @Nested
    inner class RequestInput {

        @Test
        fun `should build parameters`() {
            val schema = builder()
            schema.path("/users").operation("get") {
                parameters {
                    parameter(
                        name = "limit", location = "query",
                        description = "Max results", required = false, deprecated = false, allowEmptyValue = false,
                    ) { type("integer") }
                }
            }

            val json = schema.toJson()

            assertThatJson(json).inPath("$.paths['/users'].get.parameters[0]").isObject
                .containsEntry("name", "limit")
                .containsEntry("in", "query")
                .containsEntry("description", "Max results")
                .doesNotContainKey("required")
                .doesNotContainKey("deprecated")
                .doesNotContainKey("allowEmptyValue")
            assertThatJson(json).inPath("$.paths['/users'].get.parameters[0].schema").isObject
                .containsEntry("type", "integer")
        }

        @Test
        fun `should build parameter with example`() {
            val schema = builder()
            schema.path("/test").operation("get") {
                parameters {
                    parameter(name = "q", location = "query", example = "test-value") { type("string") }
                }
            }

            assertThatJson(schema.toJson()).inPath("$.paths['/test'].get.parameters[0]").isObject
                .containsEntry("example", "test-value")
            assertThatJson(schema.toJson()).inPath("$.paths['/test'].get.parameters[0].schema").isObject
                .containsEntry("type", "string")
                .doesNotContainKey("example")
        }

        @Test
        fun `should build request body with content`() {
            val schema = builder()
            schema.path("/users").operation("post") {
                requestBody {
                    description("User to create")
                    required(true)
                    content { mediaType("application/json") { schema { ref("#/components/schemas/User") } } }
                }
            }

            val json = schema.toJson()

            assertThatJson(json).inPath("$.paths['/users'].post.requestBody").isObject
                .containsEntry("description", "User to create")
                .containsEntry("required", true)
            assertThatJson(json).inPath("$.paths['/users'].post.requestBody.content['application/json'].schema").isObject
                .containsEntry("\$ref", "#/components/schemas/User")
        }

        @Test
        fun `should omit empty request body`() {
            val schema = builder()
            schema.path("/test").operation("get") { requestBody {} }

            assertThatJson(schema.toJson()).inPath("$.paths['/test'].get").isObject.doesNotContainKey("requestBody")
        }
    }

    @Nested
    inner class Responses {

        @Test
        fun `should build responses with content and headers`() {
            val schema = builder()
            schema.path("/users").operation("get") {
                responses {
                    response("200") {
                        description("OK")
                        content { mediaType("application/json") { schema { ref("#/components/schemas/User") } } }
                        headers { header("X-Request-Id", description = "Request ID") { type("string") } }
                    }
                    response("404") { description("Not Found") }
                }
            }

            val json = schema.toJson()

            assertThatJson(json).inPath("$.paths['/users'].get.responses['200'].description").isEqualTo("OK")
            assertThatJson(json).inPath("$.paths['/users'].get.responses['200'].content['application/json'].schema").isObject
                .containsEntry("\$ref", "#/components/schemas/User")
            assertThatJson(json).inPath("$.paths['/users'].get.responses['200'].headers['X-Request-Id']").isObject
                .containsEntry("description", "Request ID")
            assertThatJson(json).inPath("$.paths['/users'].get.responses['404'].description").isEqualTo("Not Found")
        }

        @Test
        fun `should build content with example`() {
            val schema = builder()
            schema.path("/test").operation("get") {
                responses {
                    response("200") {
                        description("OK")
                        content { mediaType("text/plain") { schema { type("string") }; example("hello world") } }
                    }
                }
            }

            assertThatJson(schema.toJson()).inPath("$.paths['/test'].get.responses['200'].content['text/plain']").isObject
                .containsEntry("example", "hello world")
        }
    }

    @Nested
    inner class MediaTypes {

        @Test
        fun `should build via lambda with resolved schema`() {
            val schema = builder()
            schema.path("/users").operation("get") {
                responses {
                    response("200") {
                        description("OK")
                        content { mediaType("application/json") { schema { ref("#/components/schemas/User") } } }
                    }
                }
            }

            assertThatJson(schema.toJson()).inPath("$.paths['/users'].get.responses['200'].content['application/json'].schema").isObject
                .containsEntry("\$ref", "#/components/schemas/User")
        }

        @Test
        fun `should build via lambda with simple schema`() {
            val schema = builder()
            schema.path("/test").operation("get") {
                responses {
                    response("200") {
                        description("OK")
                        content { mediaType("text/plain") { schema { type("string") } } }
                    }
                }
            }

            assertThatJson(schema.toJson()).inPath("$.paths['/test'].get.responses['200'].content['text/plain'].schema").isObject
                .containsEntry("type", "string")
        }

        @Test
        fun `should build via lambda with type and format`() {
            val schema = builder()
            schema.path("/test").operation("get") {
                responses {
                    response("200") {
                        description("OK")
                        content { mediaType("application/json") { schema { type("integer"); format("int32") } } }
                    }
                }
            }

            assertThatJson(schema.toJson()).inPath("$.paths['/test'].get.responses['200'].content['application/json'].schema").isObject
                .containsEntry("type", "integer")
                .containsEntry("format", "int32")
        }

        @Test
        fun `should build via lambda with ref`() {
            val schema = builder()
            schema.path("/test").operation("get") {
                responses {
                    response("200") {
                        description("OK")
                        content { mediaType("application/json") { schema { ref("#/components/schemas/User") } } }
                    }
                }
            }

            assertThatJson(schema.toJson()).inPath("$.paths['/test'].get.responses['200'].content['application/json'].schema").isObject
                .containsEntry("\$ref", "#/components/schemas/User")
                .doesNotContainKey("type")
        }

        @Test
        fun `should build via lambda with example`() {
            val schema = builder()
            schema.path("/test").operation("get") {
                responses {
                    response("200") {
                        description("OK")
                        content {
                            mediaType("text/plain") {
                                schema { type("string") }
                                example("hello world")
                            }
                        }
                    }
                }
            }

            val json = schema.toJson()

            assertThatJson(json).inPath("$.paths['/test'].get.responses['200'].content['text/plain']").isObject
                .containsEntry("example", "hello world")
            assertThatJson(json).inPath("$.paths['/test'].get.responses['200'].content['text/plain'].schema").isObject
                .containsEntry("type", "string")
        }

        @Test
        fun `should build via lambda with json example`() {
            val exampleJson = createArrayNode().apply { add("item1"); add("item2") }
            val schema = builder()
            schema.path("/test").operation("get") {
                responses {
                    response("200") {
                        description("OK")
                        content {
                            mediaType("application/json") {
                                schema { type("array") }
                                exampleJson(exampleJson)
                            }
                        }
                    }
                }
            }

            assertThatJson(schema.toJson()).inPath("$.paths['/test'].get.responses['200'].content['application/json'].example")
                .isArray.containsExactly("item1", "item2")
        }

        @Test
        fun `should omit schema when no schema set`() {
            val schema = builder()
            schema.path("/test").operation("get") {
                responses {
                    response("200") {
                        description("OK")
                        content { mediaType("application/json") { example("test") } }
                    }
                }
            }

            assertThatJson(schema.toJson()).inPath("$.paths['/test'].get.responses['200'].content['application/json']").isObject
                .doesNotContainKey("schema")
                .containsEntry("example", "test")
        }

        @Test
        fun `should omit empty simple schema`() {
            val schema = builder()
            schema.path("/test").operation("get") {
                responses {
                    response("200") {
                        description("OK")
                        content { mediaType("application/json") { schema { } } }
                    }
                }
            }

            assertThatJson(schema.toJson()).inPath("$.paths['/test'].get.responses['200'].content['application/json']").isObject
                .doesNotContainKey("schema")
        }
    }

    @Nested
    inner class ObjectSchemas {

        @Test
        fun `should build with properties`() {
            val schema = builder()
            schema.path("/test").operation("post") {
                requestBody {
                    content {
                        mediaType("application/json") {
                            objectSchema {
                                property("name", "string", null)
                                property("age", "integer", "int32")
                            }
                        }
                    }
                    required(true)
                }
            }

            val json = schema.toJson()
            val basePath = "$.paths['/test'].post.requestBody.content['application/json'].schema"

            assertThatJson(json).inPath(basePath).isObject.containsEntry("type", "object")
            assertThatJson(json).inPath("$basePath.properties.name").isObject.containsEntry("type", "string")
            assertThatJson(json).inPath("$basePath.properties.age").isObject
                .containsEntry("type", "integer")
                .containsEntry("format", "int32")
        }

        @Test
        fun `should build with resolved property`() {
            val schema = builder()
            schema.path("/test").operation("post") {
                requestBody {
                    content {
                        mediaType("application/json") {
                            objectSchema {
                                property("name", "string", null)
                                property("address") { ref("#/components/schemas/Address") }
                            }
                        }
                    }
                    required(true)
                }
            }

            assertThatJson(schema.toJson())
                .inPath("$.paths['/test'].post.requestBody.content['application/json'].schema.properties.address").isObject
                .containsEntry("\$ref", "#/components/schemas/Address")
        }

        @Test
        fun `should build with array properties`() {
            val schema = builder()
            schema.path("/test").operation("post") {
                requestBody {
                    content {
                        mediaType("application/json") {
                            objectSchema {
                                arrayProperty("tags") { ref("#/components/schemas/Tag") }
                                arrayProperty("scores", "integer", "int32")
                            }
                        }
                    }
                    required(true)
                }
            }

            val json = schema.toJson()
            val basePath = "$.paths['/test'].post.requestBody.content['application/json'].schema.properties"

            assertThatJson(json).inPath("$basePath.tags").isObject.containsEntry("type", "array")
            assertThatJson(json).inPath("$basePath.tags.items").isObject.containsEntry("\$ref", "#/components/schemas/Tag")
            assertThatJson(json).inPath("$basePath.scores").isObject.containsEntry("type", "array")
            assertThatJson(json).inPath("$basePath.scores.items").isObject
                .containsEntry("type", "integer")
                .containsEntry("format", "int32")
        }

        @Test
        fun `should build with additional properties`() {
            val schema = builder()
            schema.path("/test").operation("post") {
                requestBody {
                    content {
                        mediaType("application/json") { objectSchema { additionalProperties("string", null) } }
                    }
                    required(true)
                }
            }

            val json = schema.toJson()
            val basePath = "$.paths['/test'].post.requestBody.content['application/json'].schema"

            assertThatJson(json).inPath(basePath).isObject.containsEntry("type", "object")
            assertThatJson(json).inPath("$basePath.additionalProperties").isObject.containsEntry("type", "string")
        }

        @Test
        fun `should build with resolved additional properties`() {
            val schema = builder()
            schema.path("/test").operation("post") {
                requestBody {
                    content {
                        mediaType("application/json") {
                            objectSchema { additionalProperties { ref("#/components/schemas/Value") } }
                        }
                    }
                    required(true)
                }
            }

            assertThatJson(schema.toJson())
                .inPath("$.paths['/test'].post.requestBody.content['application/json'].schema.additionalProperties").isObject
                .containsEntry("\$ref", "#/components/schemas/Value")
        }

        @Test
        fun `should build with example`() {
            val schema = builder()
            schema.path("/test").operation("post") {
                requestBody {
                    content {
                        mediaType("application/json") {
                            objectSchema {
                                additionalProperties("string", null)
                                example("example value")
                            }
                        }
                    }
                    required(true)
                }
            }

            assertThatJson(schema.toJson())
                .inPath("$.paths['/test'].post.requestBody.content['application/json'].schema").isObject
                .containsEntry("type", "object")
                .containsEntry("example", "example value")
        }
    }

    @Nested
    inner class Callbacks {

        @Test
        fun `should build callbacks`() {
            val schema = builder()
            schema.path("/subscribe").operation("post") {
                callbacks {
                    callback("onData", "{request.body#/url}/callback", "post") {
                        summary("Callback summary")
                        description("Callback description")
                        requestBody {
                            content { mediaType("text/plain") { schema { type("string") } } }
                            required(false)
                        }
                        responses { response("200") { description("OK") } }
                    }
                }
            }

            val json = schema.toJson()
            val basePath = "$.paths['/subscribe'].post.callbacks.onData['{request.body#/url}/callback'].post"

            assertThatJson(json).inPath(basePath).isObject
                .containsEntry("summary", "Callback summary")
                .containsEntry("description", "Callback description")
            assertThatJson(json).inPath("$basePath.responses['200'].description").isEqualTo("OK")
        }
    }

    @Nested
    inner class Components {

        @Test
        fun `should add component schemas`() {
            val userSchema = ResultScheme(createObjectNode().apply {
                put("type", "object")
                set<JsonNode>("properties", createObjectNode().apply {
                    set<JsonNode>("name", createObjectNode().apply { put("type", "string") })
                })
            }, emptySet())

            val schema = builder()
            schema.addComponentSchema("User", userSchema)

            assertThatJson(schema.toJson()).inPath("$.components.schemas.User").isObject.containsEntry("type", "object")
        }

        @Test
        fun `should track component schema existence`() {
            val schema = builder()
            assert(!schema.hasComponentSchema("User"))
            schema.addComponentSchema("User", ResultScheme(createObjectNode(), emptySet()))
            assert(schema.hasComponentSchema("User"))
        }

        @Test
        fun `should resolve component references`() {
            val schema = builder()

            val addressDef = ClassDefinition(simpleName = "Address", fullName = "com.example.Address")
            val addressSchema = ResultScheme(createObjectNode().apply {
                put("type", "object")
                set<JsonNode>("properties", createObjectNode().apply {
                    set<JsonNode>("street", createObjectNode().apply { put("type", "string") })
                })
            }, emptySet())

            // Add a path that references Address via $ref
            schema.path("/users").operation("get") {
                responses {
                    response("200") {
                        description("OK")
                        content {
                            mediaType("application/json") {
                                schema(ResultScheme(
                                    createObjectNode().apply { put("\$ref", "#/components/schemas/Address") },
                                    setOf(addressDef)
                                ))
                            }
                        }
                    }
                }
            }

            schema.resolveComponentReferences { addressSchema }

            assertThatJson(schema.toJson()).inPath("$.components.schemas.Address").isObject
                .containsEntry("type", "object")
            assertThatJson(schema.toJson()).inPath("$.components.schemas.Address.properties.street").isObject
                .containsEntry("type", "string")
        }

        @Test
        fun `should resolve transitive component references`() {
            val schema = builder()

            val userDef = ClassDefinition(simpleName = "User", fullName = "com.example.User")
            val addressDef = ClassDefinition(simpleName = "Address", fullName = "com.example.Address")

            // User references Address
            val userSchema = ResultScheme(createObjectNode().apply {
                put("type", "object")
                set<JsonNode>("properties", createObjectNode().apply {
                    set<JsonNode>("address", createObjectNode().apply { put("\$ref", "#/components/schemas/Address") })
                })
            }, setOf(addressDef))

            val addressSchema = ResultScheme(createObjectNode().apply {
                put("type", "object")
                set<JsonNode>("properties", createObjectNode().apply {
                    set<JsonNode>("city", createObjectNode().apply { put("type", "string") })
                })
            }, emptySet())

            schema.path("/users").operation("get") {
                responses {
                    response("200") {
                        description("OK")
                        content {
                            mediaType("application/json") {
                                schema(ResultScheme(
                                    createObjectNode().apply { put("\$ref", "#/components/schemas/User") },
                                    setOf(userDef)
                                ))
                            }
                        }
                    }
                }
            }

            schema.resolveComponentReferences { type ->
                when (type.fullName) {
                    "com.example.User" -> userSchema
                    "com.example.Address" -> addressSchema
                    else -> ResultScheme(createObjectNode(), emptySet())
                }
            }

            assertThatJson(schema.toJson()).inPath("$.components.schemas.User").isObject.containsEntry("type", "object")
            assertThatJson(schema.toJson()).inPath("$.components.schemas.Address").isObject.containsEntry("type", "object")
        }

        @Test
        fun `should skip java-lang-Object references`() {
            val schema = builder()

            val objectDef = ClassDefinition(simpleName = "Object", fullName = "java.lang.Object")

            schema.path("/test").operation("get") {
                responses {
                    response("200") {
                        description("OK")
                        content {
                            mediaType("application/json") {
                                schema(ResultScheme(
                                    createObjectNode().apply { put("\$ref", "#/components/schemas/Object") },
                                    setOf(objectDef)
                                ))
                            }
                        }
                    }
                }
            }

            schema.resolveComponentReferences { ResultScheme(createObjectNode(), emptySet()) }

            assertThatJson(schema.toJson()).inPath("$.components.schemas").isObject
                .doesNotContainKey("Object")
        }

        @Test
        fun `should resolve circular references between types`() {
            val schema = builder()

            val aDef = ClassDefinition(simpleName = "A", fullName = "com.example.A")
            val bDef = ClassDefinition(simpleName = "B", fullName = "com.example.B")

            schema.path("/test").operation("get") {
                responses {
                    response("200") {
                        description("OK")
                        content {
                            mediaType("application/json") {
                                schema(ResultScheme(
                                    createObjectNode().apply { put("\$ref", "#/components/schemas/A") },
                                    setOf(aDef)
                                ))
                            }
                        }
                    }
                }
            }

            schema.resolveComponentReferences { type ->
                when (type.fullName) {
                    "com.example.A" -> ResultScheme(createObjectNode().apply { put("type", "object") }, setOf(bDef))
                    "com.example.B" -> ResultScheme(createObjectNode().apply { put("type", "object") }, setOf(aDef))
                    else -> ResultScheme(createObjectNode(), emptySet())
                }
            }

            assertThatJson(schema.toJson()).inPath("$.components.schemas.A").isObject.containsEntry("type", "object")
            assertThatJson(schema.toJson()).inPath("$.components.schemas.B").isObject.containsEntry("type", "object")
        }

        @Test
        fun `should resolve deep transitive chain`() {
            val schema = builder()

            val aDef = ClassDefinition(simpleName = "A", fullName = "com.example.A")

            schema.path("/test").operation("get") {
                responses {
                    response("200") {
                        description("OK")
                        content {
                            mediaType("application/json") {
                                schema(ResultScheme(
                                    createObjectNode().apply { put("\$ref", "#/components/schemas/A") },
                                    setOf(aDef)
                                ))
                            }
                        }
                    }
                }
            }

            // A -> B -> C (chain of 3)
            val bDef = ClassDefinition(simpleName = "B", fullName = "com.example.B")
            val cDef = ClassDefinition(simpleName = "C", fullName = "com.example.C")

            schema.resolveComponentReferences { type ->
                when (type.fullName) {
                    "com.example.A" -> ResultScheme(createObjectNode().apply { put("type", "object") }, setOf(bDef))
                    "com.example.B" -> ResultScheme(createObjectNode().apply { put("type", "object") }, setOf(cDef))
                    "com.example.C" -> ResultScheme(createObjectNode().apply { put("type", "object") }, emptySet())
                    else -> ResultScheme(createObjectNode(), emptySet())
                }
            }

            assertThatJson(schema.toJson()).inPath("$.components.schemas").isObject
                .containsKey("A")
                .containsKey("B")
                .containsKey("C")
        }

        @Test
        fun `should preserve non-schema components in toJson`() {
            val schema = builder().withBasicAuth()
            schema.addComponentSchema("User", ResultScheme(createObjectNode().apply { put("type", "object") }, emptySet()))

            val json = schema.toJson()

            assertThatJson(json).inPath("$.components.securitySchemes.BasicAuth").isObject.containsEntry("type", "http")
            assertThatJson(json).inPath("$.components.schemas.User").isObject.containsEntry("type", "object")
        }
    }

    @Nested
    inner class Serialization {

        @Test
        fun `should round-trip fromJson`() {
            val original = OpenApiSchemaBuilder()
                .openApiVersion("3.1.0")
                .info { it.title("My API").version("1.0") }
            original.path("/users").operation("get") { summary("List users") }

            val roundTripped = OpenApiSchemaBuilder.fromJson(original.toJson()).toJson()

            assertThatJson(roundTripped).isObject.containsEntry("openapi", "3.1.0")
            assertThatJson(roundTripped).inPath("$.info").isObject
                .containsEntry("title", "My API")
                .containsEntry("version", "1.0")
            assertThatJson(roundTripped).inPath("$.paths['/users'].get.summary").isEqualTo("List users")
        }

        @Test
        fun `should modify schema after fromJson`() {
            val original = """{"openapi":"3.1.0","info":{"title":"API","version":"1.0"},"paths":{},"components":{"schemas":{}}}"""
            val schema = OpenApiSchemaBuilder.fromJson(original)
            schema.path("/new").operation("get") { summary("New endpoint") }

            val json = schema.toJson()

            assertThatJson(json).inPath("$.paths['/new'].get.summary").isEqualTo("New endpoint")
            assertThatJson(json).inPath("$.openapi").isEqualTo("3.1.0")
        }
    }

    @Nested
    inner class MergeBehavior {

        @Test
        fun `should preserve all fields when reopening operation with empty lambda`() {
            val schema = builder()
            schema.path("/users").operation("get") {
                tags("users")
                summary("List users")
                description("Returns all users")
                operationId("getUsers")
                deprecated(true)
                parameters { parameter("limit", "query") { type("integer") } }
                responses { response("200") { description("OK") } }
                security { securityRequirement("BearerAuth") }
            }

            schema.path("/users").operation("get") {}

            val json = schema.toJson()

            assertThatJson(json).inPath("$.paths['/users'].get.tags").isArray.containsExactly("users")
            assertThatJson(json).inPath("$.paths['/users'].get.summary").isEqualTo("List users")
            assertThatJson(json).inPath("$.paths['/users'].get.description").isEqualTo("Returns all users")
            assertThatJson(json).inPath("$.paths['/users'].get.operationId").isEqualTo("getUsers")
            assertThatJson(json).inPath("$.paths['/users'].get.deprecated").isEqualTo(true)
            assertThatJson(json).inPath("$.paths['/users'].get.parameters").isArray.hasSize(1)
            assertThatJson(json).inPath("$.paths['/users'].get.responses['200'].description").isEqualTo("OK")
            assertThatJson(json).inPath("$.paths['/users'].get.security").isArray.hasSize(1)
        }

        @Test
        fun `should merge only changed field when reopening operation`() {
            val schema = builder()
            schema.path("/users").operation("get") {
                summary("List users")
                operationId("getUsers")
                responses { response("200") { description("OK") } }
            }

            schema.path("/users").operation("get") { summary("Updated summary") }

            val json = schema.toJson()

            assertThatJson(json).inPath("$.paths['/users'].get.summary").isEqualTo("Updated summary")
            assertThatJson(json).inPath("$.paths['/users'].get.operationId").isEqualTo("getUsers")
            assertThatJson(json).inPath("$.paths['/users'].get.responses['200'].description").isEqualTo("OK")
        }

        @Test
        fun `should not affect other methods when reopening one method`() {
            val schema = builder()
            schema.path("/users").operation("get") {
                summary("List users")
                responses { response("200") { description("OK") } }
            }
            schema.path("/users").operation("post") {
                summary("Create user")
                responses { response("201") { description("Created") } }
            }

            schema.path("/users").operation("get") { summary("Updated list users") }

            val json = schema.toJson()

            assertThatJson(json).inPath("$.paths['/users'].get.summary").isEqualTo("Updated list users")
            assertThatJson(json).inPath("$.paths['/users'].get.responses['200'].description").isEqualTo("OK")
            assertThatJson(json).inPath("$.paths['/users'].post.summary").isEqualTo("Create user")
            assertThatJson(json).inPath("$.paths['/users'].post.responses['201'].description").isEqualTo("Created")
        }

        @Test
        fun `should replace tags when explicitly called`() {
            val schema = builder()
            schema.path("/users").operation("get") { tags("users", "admin") }
            schema.path("/users").operation("get") { tags("public") }

            assertThatJson(schema.toJson()).inPath("$.paths['/users'].get.tags").isArray.containsExactly("public")
        }

        @Test
        fun `should append tags with addTag`() {
            val schema = builder()
            schema.path("/users").operation("get") { tags("users") }
            schema.path("/users").operation("get") { addTag("admin") }

            assertThatJson(schema.toJson()).inPath("$.paths['/users'].get.tags").isArray
                .containsExactly("users", "admin")
        }

        @Test
        fun `should append tags with addTags`() {
            val schema = builder()
            schema.path("/users").operation("get") { tags("users") }
            schema.path("/users").operation("get") { addTags(listOf("admin", "public")) }

            assertThatJson(schema.toJson()).inPath("$.paths['/users'].get.tags").isArray
                .containsExactly("users", "admin", "public")
        }

        @Test
        fun `should omit deprecated when not set`() {
            val schema = builder()
            schema.path("/test").operation("get") { summary("Test") }

            assertThatJson(schema.toJson()).inPath("$.paths['/test'].get").isObject
                .doesNotContainKey("deprecated")
        }

        @Test
        fun `should preserve direct mediaType schema on merge`() {
            val schema = builder()
            schema.path("/test").operation("get") {
                responses {
                    response("200") {
                        description("OK")
                        content { mediaType("application/json") { schema { type("string") } } }
                    }
                }
            }

            schema.path("/test").operation("get") {
                responses {
                    response("200") {
                        content { mediaType("application/json") { example("hello") } }
                    }
                }
            }

            val json = schema.toJson()

            assertThatJson(json).inPath("$.paths['/test'].get.responses['200'].content['application/json'].schema.type")
                .isEqualTo("string")
            assertThatJson(json).inPath("$.paths['/test'].get.responses['200'].content['application/json'].example")
                .isEqualTo("hello")
        }

        @Test
        fun `should preserve unknown extension fields`() {
            val jsonWithExtensions = """
            {
                "openapi": "3.1.0",
                "info": { "title": "", "version": "" },
                "paths": {
                    "/test": {
                        "get": {
                            "summary": "Test",
                            "x-custom-field": "custom-value",
                            "externalDocs": { "url": "https://docs.example.com" },
                            "responses": {}
                        }
                    }
                },
                "components": { "schemas": {} }
            }
            """.trimIndent()

            val schema = OpenApiSchemaBuilder.fromJson(jsonWithExtensions)
            schema.path("/test").operation("get") { summary("Updated") }

            val json = schema.toJson()

            assertThatJson(json).inPath("$.paths['/test'].get.summary").isEqualTo("Updated")
            assertThatJson(json).inPath("$.paths['/test'].get.x-custom-field").isEqualTo("custom-value")
            assertThatJson(json).inPath("$.paths['/test'].get.externalDocs.url").isEqualTo("https://docs.example.com")
        }

        @Nested
        inner class Parameters {

            @Test
            fun `should append parameters with different names`() {
                val schema = builder()
                schema.path("/users").operation("get") {
                    parameters { parameter("limit", "query") { type("integer") } }
                }
                schema.path("/users").operation("get") {
                    parameters { parameter("name", "query") { type("string") } }
                }

                val json = schema.toJson()

                assertThatJson(json).inPath("$.paths['/users'].get.parameters").isArray.hasSize(2)
                assertThatJson(json).inPath("$.paths['/users'].get.parameters[0].name").isEqualTo("limit")
                assertThatJson(json).inPath("$.paths['/users'].get.parameters[1].name").isEqualTo("name")
            }

            @Test
            fun `should replace parameter with same name and location`() {
                val schema = builder()
                schema.path("/users").operation("get") {
                    parameters {
                        parameter("limit", "query", description = "Old desc") { type("integer") }
                    }
                }
                schema.path("/users").operation("get") {
                    parameters {
                        parameter("limit", "query", description = "New desc") { type("integer") }
                    }
                }

                val json = schema.toJson()

                assertThatJson(json).inPath("$.paths['/users'].get.parameters").isArray.hasSize(1)
                assertThatJson(json).inPath("$.paths['/users'].get.parameters[0].description").isEqualTo("New desc")
            }

            @Test
            fun `should allow same parameter name in different locations`() {
                val schema = builder()
                schema.path("/users").operation("get") {
                    parameters {
                        parameter("id", "query") { type("string") }
                        parameter("id", "header") { type("string") }
                    }
                }

                val json = schema.toJson()

                assertThatJson(json).inPath("$.paths['/users'].get.parameters").isArray.hasSize(2)
                assertThatJson(json).inPath("$.paths['/users'].get.parameters[0].in").isEqualTo("query")
                assertThatJson(json).inPath("$.paths['/users'].get.parameters[1].in").isEqualTo("header")
            }
        }

        @Nested
        inner class RequestBody {

            @Test
            fun `should preserve request body when reopened with empty lambda`() {
                val schema = builder()
                schema.path("/users").operation("post") {
                    requestBody {
                        description("User to create")
                        required(true)
                        content { mediaType("application/json") { schema { ref("#/components/schemas/User") } } }
                    }
                }

                schema.path("/users").operation("post") { requestBody {} }

                val json = schema.toJson()

                assertThatJson(json).inPath("$.paths['/users'].post.requestBody.description").isEqualTo("User to create")
                assertThatJson(json).inPath("$.paths['/users'].post.requestBody.required").isEqualTo(true)
                assertThatJson(json).inPath("$.paths['/users'].post.requestBody.content['application/json'].schema.\$ref")
                    .isEqualTo("#/components/schemas/User")
            }

            @Test
            fun `should add content type to existing request body`() {
                val schema = builder()
                schema.path("/users").operation("post") {
                    requestBody {
                        description("User payload")
                        required(true)
                        content { mediaType("application/json") { schema { ref("#/components/schemas/User") } } }
                    }
                }

                schema.path("/users").operation("post") {
                    requestBody {
                        content { mediaType("application/xml") { schema { type("object") } } }
                    }
                }

                val json = schema.toJson()

                assertThatJson(json).inPath("$.paths['/users'].post.requestBody.description").isEqualTo("User payload")
                assertThatJson(json).inPath("$.paths['/users'].post.requestBody.required").isEqualTo(true)
                assertThatJson(json).inPath("$.paths['/users'].post.requestBody.content['application/json'].schema.\$ref")
                    .isEqualTo("#/components/schemas/User")
                assertThatJson(json).inPath("$.paths['/users'].post.requestBody.content['application/xml'].schema.type")
                    .isEqualTo("object")
            }
        }

        @Nested
        inner class ResponsesMerge {

            @Test
            fun `should add new response status while preserving existing ones`() {
                val schema = builder()
                schema.path("/users").operation("get") {
                    responses { response("200") { description("OK") } }
                }
                schema.path("/users").operation("get") {
                    responses { response("404") { description("Not Found") } }
                }

                val json = schema.toJson()

                assertThatJson(json).inPath("$.paths['/users'].get.responses['200'].description").isEqualTo("OK")
                assertThatJson(json).inPath("$.paths['/users'].get.responses['404'].description").isEqualTo("Not Found")
            }

            @Test
            fun `should merge response content when reopening same status code`() {
                val schema = builder()
                schema.path("/users").operation("get") {
                    responses {
                        response("200") {
                            description("OK")
                            content { mediaType("application/json") { schema { ref("#/components/schemas/User") } } }
                        }
                    }
                }

                schema.path("/users").operation("get") {
                    responses {
                        response("200") {
                            content { mediaType("application/xml") { schema { type("object") } } }
                        }
                    }
                }

                val json = schema.toJson()

                assertThatJson(json).inPath("$.paths['/users'].get.responses['200'].description").isEqualTo("OK")
                assertThatJson(json).inPath("$.paths['/users'].get.responses['200'].content['application/json'].schema.\$ref")
                    .isEqualTo("#/components/schemas/User")
                assertThatJson(json).inPath("$.paths['/users'].get.responses['200'].content['application/xml'].schema.type")
                    .isEqualTo("object")
            }

            @Test
            fun `should preserve media type schema when adding example`() {
                val schema = builder()
                schema.path("/test").operation("get") {
                    responses {
                        response("200") {
                            description("OK")
                            content { mediaType("application/json") { schema { type("string") } } }
                        }
                    }
                }

                schema.path("/test").operation("get") {
                    responses {
                        response("200") {
                            content { mediaType("application/json") { example("hello") } }
                        }
                    }
                }

                val json = schema.toJson()

                assertThatJson(json).inPath("$.paths['/test'].get.responses['200'].content['application/json'].schema.type")
                    .isEqualTo("string")
                assertThatJson(json).inPath("$.paths['/test'].get.responses['200'].content['application/json'].example")
                    .isEqualTo("hello")
            }

            @Test
            fun `should merge headers when reopening response`() {
                val schema = builder()
                schema.path("/test").operation("get") {
                    responses {
                        response("200") {
                            description("OK")
                            headers { header("X-Request-Id") { type("string") } }
                        }
                    }
                }

                schema.path("/test").operation("get") {
                    responses {
                        response("200") {
                            headers { header("X-Rate-Limit") { type("integer") } }
                        }
                    }
                }

                val json = schema.toJson()

                assertThatJson(json).inPath("$.paths['/test'].get.responses['200'].headers['X-Request-Id'].schema.type")
                    .isEqualTo("string")
                assertThatJson(json).inPath("$.paths['/test'].get.responses['200'].headers['X-Rate-Limit'].schema.type")
                    .isEqualTo("integer")
            }
        }

        @Nested
        inner class SecurityMerge {

            @Test
            fun `should append security requirements when reopening operation`() {
                val schema = builder()
                schema.path("/secure").operation("get") {
                    security { securityRequirement("BearerAuth") }
                }
                schema.path("/secure").operation("get") {
                    security { securityRequirement("OAuth2", "read") }
                }

                val json = schema.toJson()

                assertThatJson(json).inPath("$.paths['/secure'].get.security").isArray.hasSize(2)
                assertThatJson(json).inPath("$.paths['/secure'].get.security[0].BearerAuth").isArray.isEmpty()
                assertThatJson(json).inPath("$.paths['/secure'].get.security[1].OAuth2").isArray.containsExactly("read")
            }
        }

        @Nested
        inner class CallbacksMerge {

            @Test
            fun `should merge callbacks when reopening operation`() {
                val schema = builder()
                schema.path("/subscribe").operation("post") {
                    callbacks {
                        callback("onData", "{url}/data", "post") {
                            summary("Data callback")
                            responses { response("200") { description("OK") } }
                        }
                    }
                }

                schema.path("/subscribe").operation("post") {
                    callbacks {
                        callback("onError", "{url}/error", "post") { summary("Error callback") }
                    }
                }

                val json = schema.toJson()

                assertThatJson(json).inPath("$.paths['/subscribe'].post.callbacks.onData['{url}/data'].post.summary")
                    .isEqualTo("Data callback")
                assertThatJson(json).inPath("$.paths['/subscribe'].post.callbacks.onError['{url}/error'].post.summary")
                    .isEqualTo("Error callback")
            }

            @Test
            fun `should merge callback operation fields when reopening same callback`() {
                val schema = builder()
                schema.path("/subscribe").operation("post") {
                    callbacks {
                        callback("onData", "{url}/cb", "post") {
                            summary("Original summary")
                            description("Original description")
                        }
                    }
                }

                schema.path("/subscribe").operation("post") {
                    callbacks {
                        callback("onData", "{url}/cb", "post") { summary("Updated summary") }
                    }
                }

                val json = schema.toJson()

                assertThatJson(json).inPath("$.paths['/subscribe'].post.callbacks.onData['{url}/cb'].post.summary")
                    .isEqualTo("Updated summary")
                assertThatJson(json).inPath("$.paths['/subscribe'].post.callbacks.onData['{url}/cb'].post.description")
                    .isEqualTo("Original description")
            }
        }

        @Test
        fun `should extend compile-time generated schema at runtime via fromJson`() {
            val compileTimeJson = """
            {
                "openapi": "3.1.0",
                "info": { "title": "Generated API", "version": "1.0" },
                "paths": {
                    "/users": {
                        "get": {
                            "tags": ["users"],
                            "summary": "Get users",
                            "operationId": "getUsers",
                            "parameters": [
                                { "name": "page", "in": "query", "schema": { "type": "integer" } }
                            ],
                            "responses": {
                                "200": {
                                    "description": "OK",
                                    "content": {
                                        "application/json": {
                                            "schema": { "${'$'}ref": "#/components/schemas/User" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                "components": { "schemas": { "User": { "type": "object" } } }
            }
            """.trimIndent()

            val schema = OpenApiSchemaBuilder.fromJson(compileTimeJson)
            schema.server { it.url("https://api.example.com") }.withBearerAuth()
            schema.path("/users").operation("get") {
                security { securityRequirement("BearerAuth") }
            }

            val json = schema.toJson()

            // Compile-time data preserved
            assertThatJson(json).inPath("$.paths['/users'].get.tags").isArray.containsExactly("users")
            assertThatJson(json).inPath("$.paths['/users'].get.summary").isEqualTo("Get users")
            assertThatJson(json).inPath("$.paths['/users'].get.operationId").isEqualTo("getUsers")
            assertThatJson(json).inPath("$.paths['/users'].get.parameters[0].name").isEqualTo("page")
            assertThatJson(json).inPath("$.paths['/users'].get.responses['200'].description").isEqualTo("OK")
            assertThatJson(json).inPath("$.paths['/users'].get.responses['200'].content['application/json'].schema.\$ref")
                .isEqualTo("#/components/schemas/User")

            // Runtime additions
            assertThatJson(json).inPath("$.servers[0].url").isEqualTo("https://api.example.com")
            assertThatJson(json).inPath("$.components.securitySchemes.BearerAuth.scheme").isEqualTo("bearer")
            assertThatJson(json).inPath("$.paths['/users'].get.security[0].BearerAuth").isArray.isEmpty()
        }
    }
}
