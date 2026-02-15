package io.javalin.openapi.schema

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.javalin.openapi.experimental.processor.generators.ResultScheme
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test

internal class OpenApiSchemaBuilderTest {

    private fun resultScheme(configure: JsonObject.() -> Unit = {}): ResultScheme =
        ResultScheme(JsonObject().apply(configure), emptySet())

    @Test
    fun `should build minimal document`() {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "My API", version = "1.0")

        val json = schema.toJson()

        assertThatJson(json)
            .isObject
            .containsEntry("openapi", "3.0.3")
            .containsEntry("info", json("""{ "title": "My API", "version": "1.0" }"""))
            .containsEntry("paths", json("{}"))
            .containsEntry("components", json("""{ "schemas": {} }"""))
    }

    @Test
    fun `should build empty info when null values`() {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = null, version = null)

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.info")
            .isObject
            .containsEntry("title", "")
            .containsEntry("version", "")
    }

    @Test
    fun `should build path with operation`() {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/users").operation("get") {
            tags("users", "admin")
            summary("Get all users")
            description("Returns a list of users")
            operationId("getUsers")
            deprecated(false)
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/users'].get")
            .isObject
            .containsEntry("tags", json("""["users", "admin"]"""))
            .containsEntry("summary", "Get all users")
            .containsEntry("description", "Returns a list of users")
            .containsEntry("operationId", "getUsers")
            .containsEntry("deprecated", false)
            .containsEntry("parameters", json("[]"))
            .containsEntry("responses", json("{}"))
            .containsEntry("security", json("[]"))
    }

    @Test
    fun `should omit null summary and description`() {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/test").operation("get") {
            summary(null)
            description(null)
            operationId(null)
            deprecated(false)
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/test'].get")
            .isObject
            .doesNotContainKey("summary")
            .doesNotContainKey("description")
            .doesNotContainKey("operationId")
    }

    @Test
    fun `should build parameters`() {
        val intSchema = resultScheme { addProperty("type", "integer") }

        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/users").operation("get") {
            deprecated(false)
            parameters {
                parameter(
                    name = "limit",
                    location = "query",
                    schema = intSchema,
                    description = "Max results",
                    required = false,
                    deprecated = false,
                    allowEmptyValue = false,
                )
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/users'].get.parameters[0]")
            .isObject
            .containsEntry("name", "limit")
            .containsEntry("in", "query")
            .containsEntry("description", "Max results")
            .containsEntry("required", false)
            .containsEntry("deprecated", false)
            .containsEntry("allowEmptyValue", false)

        assertThatJson(json)
            .inPath("$.paths['/users'].get.parameters[0].schema")
            .isObject
            .containsEntry("type", "integer")
    }

    @Test
    fun `should build parameter with example`() {
        val stringSchema = resultScheme { addProperty("type", "string") }

        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/test").operation("get") {
            deprecated(false)
            parameters {
                parameter(
                    name = "q",
                    location = "query",
                    schema = stringSchema,
                    example = "test-value",
                )
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/test'].get.parameters[0].schema")
            .isObject
            .containsEntry("type", "string")
            .containsEntry("example", "test-value")
    }

    @Test
    fun `should build request body with content`() {
        val userSchema = resultScheme { addProperty("\$ref", "#/components/schemas/User") }

        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/users").operation("post") {
            deprecated(false)
            requestBody {
                description("User to create")
                required(true)
                content {
                    mediaType("application/json", schema = userSchema)
                }
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/users'].post.requestBody")
            .isObject
            .containsEntry("description", "User to create")
            .containsEntry("required", true)

        assertThatJson(json)
            .inPath("$.paths['/users'].post.requestBody.content['application/json'].schema")
            .isObject
            .containsEntry("\$ref", "#/components/schemas/User")
    }

    @Test
    fun `should omit empty request body`() {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/test").operation("get") {
            deprecated(false)
            requestBody {
                // empty - no content, no description
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/test'].get")
            .isObject
            .doesNotContainKey("requestBody")
    }

    @Test
    fun `should build responses with content and headers`() {
        val userSchema = resultScheme { addProperty("\$ref", "#/components/schemas/User") }
        val headerSchema = resultScheme { addProperty("type", "string") }

        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/users").operation("get") {
            deprecated(false)
            responses {
                response("200") {
                    description("OK")
                    content {
                        mediaType("application/json", schema = userSchema)
                    }
                    headers {
                        header("X-Request-Id", schema = headerSchema, description = "Request ID")
                    }
                }
                response("404") {
                    description("Not Found")
                }
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/users'].get.responses['200'].description")
            .isEqualTo("OK")

        assertThatJson(json)
            .inPath("$.paths['/users'].get.responses['200'].content['application/json'].schema")
            .isObject
            .containsEntry("\$ref", "#/components/schemas/User")

        assertThatJson(json)
            .inPath("$.paths['/users'].get.responses['200'].headers['X-Request-Id']")
            .isObject
            .containsEntry("description", "Request ID")

        assertThatJson(json)
            .inPath("$.paths['/users'].get.responses['404'].description")
            .isEqualTo("Not Found")
    }

    @Test
    fun `should build callbacks`() {
        val stringSchema = resultScheme { addProperty("type", "string") }

        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/subscribe").operation("post") {
            deprecated(false)
            callbacks {
                callback("onData", "{request.body#/url}/callback", "post") {
                    summary("Callback summary")
                    description("Callback description")
                    requestBody {
                        content {
                            mediaType("text/plain", schema = stringSchema)
                        }
                        required(false)
                    }
                    responses {
                        response("200") {
                            description("OK")
                        }
                    }
                }
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/subscribe'].post.callbacks.onData['{request.body#/url}/callback'].post")
            .isObject
            .containsEntry("summary", "Callback summary")
            .containsEntry("description", "Callback description")

        assertThatJson(json)
            .inPath("$.paths['/subscribe'].post.callbacks.onData['{request.body#/url}/callback'].post.responses['200'].description")
            .isEqualTo("OK")
    }

    @Test
    fun `should build security requirements`() {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/secure").operation("get") {
            deprecated(false)
            security {
                securityRequirement("BearerAuth")
                securityRequirement("OAuth2", "read", "write")
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/secure'].get.security")
            .isArray
            .hasSize(2)

        assertThatJson(json)
            .inPath("$.paths['/secure'].get.security[0]")
            .isObject
            .containsEntry("BearerAuth", json("[]"))

        assertThatJson(json)
            .inPath("$.paths['/secure'].get.security[1]")
            .isObject
            .containsEntry("OAuth2", json("""["read", "write"]"""))
    }

    @Test
    fun `should add component schemas`() {
        val userSchema = ResultScheme(JsonObject().apply {
            addProperty("type", "object")
            val props = JsonObject()
            props.add("name", JsonObject().apply { addProperty("type", "string") })
            add("properties", props)
        }, emptySet())

        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.addComponentSchema("User", userSchema)

        assertThatJson(schema.toJson())
            .inPath("$.components.schemas.User")
            .isObject
            .containsEntry("type", "object")
    }

    @Test
    fun `should track component schema existence`() {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        assert(!schema.hasComponentSchema("User"))
        schema.addComponentSchema("User", resultScheme())
        assert(schema.hasComponentSchema("User"))
    }

    @Test
    fun `should build content with example`() {
        val stringSchema = resultScheme { addProperty("type", "string") }

        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/test").operation("get") {
            deprecated(false)
            responses {
                response("200") {
                    description("OK")
                    content {
                        mediaType("text/plain", schema = stringSchema, example = "hello world")
                    }
                }
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/test'].get.responses['200'].content['text/plain']")
            .isObject
            .containsEntry("example", "hello world")
    }

    @Test
    fun `should support multiple methods on same path`() {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        val path = schema.path("/users")
        path.operation("get") {
            summary("List users")
            deprecated(false)
        }
        path.operation("post") {
            summary("Create user")
            deprecated(false)
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/users'].get.summary")
            .isEqualTo("List users")

        assertThatJson(json)
            .inPath("$.paths['/users'].post.summary")
            .isEqualTo("Create user")
    }

    @Test
    fun `should preserve field ordering in operation`() {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/test").operation("get") {
            tags("test")
            summary("Test")
            description("Desc")
            operationId("testOp")
            deprecated(true)
            security {
                securityRequirement("auth")
            }
        }

        val json = schema.toJson()

        // Verify field order: tags, summary, description, operationId, parameters, responses, deprecated, security
        assertThatJson(json)
            .inPath("$.paths['/test'].get")
            .isObject
            .containsKeys("tags", "summary", "description", "operationId", "parameters", "responses", "deprecated", "security")
    }

    @Test
    fun `should build media type via lambda with resolved schema`() {
        val refSchema = resultScheme { addProperty("\$ref", "#/components/schemas/User") }

        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/users").operation("get") {
            deprecated(false)
            responses {
                response("200") {
                    description("OK")
                    content {
                        mediaType("application/json") {
                            schema(refSchema)
                        }
                    }
                }
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/users'].get.responses['200'].content['application/json'].schema")
            .isObject
            .containsEntry("\$ref", "#/components/schemas/User")
    }

    @Test
    fun `should build media type via lambda with simple schema`() {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/test").operation("get") {
            deprecated(false)
            responses {
                response("200") {
                    description("OK")
                    content {
                        mediaType("text/plain") {
                            simpleSchema("string", null)
                        }
                    }
                }
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/test'].get.responses['200'].content['text/plain'].schema")
            .isObject
            .containsEntry("type", "string")
    }

    @Test
    fun `should build media type via lambda with example`() {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/test").operation("get") {
            deprecated(false)
            responses {
                response("200") {
                    description("OK")
                    content {
                        mediaType("text/plain") {
                            simpleSchema("string", null)
                            example("hello world")
                        }
                    }
                }
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/test'].get.responses['200'].content['text/plain']")
            .isObject
            .containsEntry("example", "hello world")

        assertThatJson(json)
            .inPath("$.paths['/test'].get.responses['200'].content['text/plain'].schema")
            .isObject
            .containsEntry("type", "string")
    }

    @Test
    fun `should build media type via lambda with json example`() {
        val exampleJson = JsonArray().apply { add("item1"); add("item2") }

        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/test").operation("get") {
            deprecated(false)
            responses {
                response("200") {
                    description("OK")
                    content {
                        mediaType("application/json") {
                            simpleSchema("array", null)
                            exampleJson(exampleJson)
                        }
                    }
                }
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/test'].get.responses['200'].content['application/json'].example")
            .isArray
            .containsExactly("item1", "item2")
    }

    @Test
    fun `should build object schema with properties`() {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/test").operation("post") {
            deprecated(false)
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

        assertThatJson(json)
            .inPath("$.paths['/test'].post.requestBody.content['application/json'].schema")
            .isObject
            .containsEntry("type", "object")

        assertThatJson(json)
            .inPath("$.paths['/test'].post.requestBody.content['application/json'].schema.properties.name")
            .isObject
            .containsEntry("type", "string")

        assertThatJson(json)
            .inPath("$.paths['/test'].post.requestBody.content['application/json'].schema.properties.age")
            .isObject
            .containsEntry("type", "integer")
            .containsEntry("format", "int32")
    }

    @Test
    fun `should build object schema with resolved property`() {
        val refSchema = resultScheme { addProperty("\$ref", "#/components/schemas/Address") }

        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/test").operation("post") {
            deprecated(false)
            requestBody {
                content {
                    mediaType("application/json") {
                        objectSchema {
                            property("name", "string", null)
                            property("address", refSchema)
                        }
                    }
                }
                required(true)
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/test'].post.requestBody.content['application/json'].schema.properties.address")
            .isObject
            .containsEntry("\$ref", "#/components/schemas/Address")
    }

    @Test
    fun `should build object schema with array properties`() {
        val refSchema = resultScheme { addProperty("\$ref", "#/components/schemas/Tag") }

        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/test").operation("post") {
            deprecated(false)
            requestBody {
                content {
                    mediaType("application/json") {
                        objectSchema {
                            arrayProperty("tags", refSchema)
                            arrayProperty("scores", "integer", "int32")
                        }
                    }
                }
                required(true)
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/test'].post.requestBody.content['application/json'].schema.properties.tags")
            .isObject
            .containsEntry("type", "array")

        assertThatJson(json)
            .inPath("$.paths['/test'].post.requestBody.content['application/json'].schema.properties.tags.items")
            .isObject
            .containsEntry("\$ref", "#/components/schemas/Tag")

        assertThatJson(json)
            .inPath("$.paths['/test'].post.requestBody.content['application/json'].schema.properties.scores")
            .isObject
            .containsEntry("type", "array")

        assertThatJson(json)
            .inPath("$.paths['/test'].post.requestBody.content['application/json'].schema.properties.scores.items")
            .isObject
            .containsEntry("type", "integer")
            .containsEntry("format", "int32")
    }

    @Test
    fun `should build object schema with additional properties`() {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/test").operation("post") {
            deprecated(false)
            requestBody {
                content {
                    mediaType("application/json") {
                        objectSchema {
                            additionalProperties("string", null)
                        }
                    }
                }
                required(true)
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/test'].post.requestBody.content['application/json'].schema")
            .isObject
            .containsEntry("type", "object")

        assertThatJson(json)
            .inPath("$.paths['/test'].post.requestBody.content['application/json'].schema.additionalProperties")
            .isObject
            .containsEntry("type", "string")
    }

    @Test
    fun `should build object schema with resolved additional properties`() {
        val refSchema = resultScheme { addProperty("\$ref", "#/components/schemas/Value") }

        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/test").operation("post") {
            deprecated(false)
            requestBody {
                content {
                    mediaType("application/json") {
                        objectSchema {
                            additionalProperties(refSchema)
                        }
                    }
                }
                required(true)
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/test'].post.requestBody.content['application/json'].schema.additionalProperties")
            .isObject
            .containsEntry("\$ref", "#/components/schemas/Value")
    }

    @Test
    fun `should build object schema with example on parent from additional properties`() {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/test").operation("post") {
            deprecated(false)
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

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/test'].post.requestBody.content['application/json'].schema")
            .isObject
            .containsEntry("type", "object")
            .containsEntry("example", "example value")
    }

    @Test
    fun `should omit schema when media type builder has no schema set`() {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/test").operation("get") {
            deprecated(false)
            responses {
                response("200") {
                    description("OK")
                    content {
                        mediaType("application/json") {
                            // no schema set
                            example("test")
                        }
                    }
                }
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/test'].get.responses['200'].content['application/json']")
            .isObject
            .doesNotContainKey("schema")
            .containsEntry("example", "test")
    }

    @Test
    fun `should omit empty simple schema`() {
        val schema = OpenApiSchemaBuilder()
            .openApiVersion("3.0.3")
            .info(title = "", version = "")

        schema.path("/test").operation("get") {
            deprecated(false)
            responses {
                response("200") {
                    description("OK")
                    content {
                        mediaType("application/json") {
                            simpleSchema(null, null)
                        }
                    }
                }
            }
        }

        val json = schema.toJson()

        assertThatJson(json)
            .inPath("$.paths['/test'].get.responses['200'].content['application/json']")
            .isObject
            .doesNotContainKey("schema")
    }
}
