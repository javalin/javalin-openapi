package io.javalin.openapi.dynamic.hook

import io.javalin.Javalin
import io.javalin.http.HandlerType
import io.javalin.openapi.experimental.processor.shared.jsonMapper
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.openapi.plugin.redoc.ReDocPlugin
import io.javalin.openapi.plugin.swagger.SwaggerPlugin
import io.javalin.router.Endpoint
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DynamicOpenApiHookTest {

    @Test
    fun `autogenerates docs for registered user routes`() {
        val app = Javalin.start { config ->
            config.jetty.port = 0
            config.registerPlugin(OpenApiPlugin { it.withHook(RegisteredRoutesHook()) })
            config.routes.get("/users") { }
            config.routes.post("/users") { }
            config.routes.get("/users/{id}") { }
        }

        try {
            val body = Unirest.get("http://localhost:${app.port()}/openapi").asString().body
            val document = jsonMapper.readTree(body)
            val paths = document.path("paths")

            assertThat(document.path("info").has("title")).isTrue()
            assertThat(document.path("info").has("version")).isTrue()
            assertThat(paths.has("/openapi")).isFalse()
            assertThat(paths.path("/users").has("get")).isTrue()
            assertThat(paths.path("/users").has("post")).isTrue()

            val byId = paths.path("/users/{id}").path("get")
            assertThat(byId.path("responses").path("200").path("description").asText()).isEqualTo("OK")

            val idParam = byId.path("parameters")[0]
            assertThat(idParam.path("name").asText()).isEqualTo("id")
            assertThat(idParam.path("in").asText()).isEqualTo("path")
            assertThat(idParam.path("required").asBoolean()).isTrue()
            assertThat(idParam.path("schema").path("type").asText()).isEqualTo("string")
        } finally {
            app.stop()
        }
    }

    @Test
    fun `ignores routes registered by OpenAPI plugins by default`() {
        val app = Javalin.start { config ->
            config.jetty.port = 0
            config.registerPlugin(OpenApiPlugin { it.withHook(RegisteredRoutesHook()) })
            config.registerPlugin(SwaggerPlugin { swagger ->
                swagger
                    .withUiPath("/documentation/swagger")
                    .withWebJarPath("/documentation/assets/swagger")
            })
            config.registerPlugin(ReDocPlugin { redoc ->
                redoc
                    .withUiPath("/documentation/redoc")
                    .withWebJarPath("/documentation/assets/redoc")
            })
            config.routes.get("/users") { }
        }

        try {
            val document = jsonMapper.readTree(Unirest.get("http://localhost:${app.port()}/openapi").asString().body)
            val paths = document.path("paths").fieldNames().asSequence().toList()

            assertThat(paths).containsExactly("/users")
        } finally {
            app.stop()
        }
    }

    @Test
    fun `includes default ignored routes when configured`() {
        val app = Javalin.start { config ->
            config.jetty.port = 0
            config.registerPlugin(
                OpenApiPlugin {
                    it.withHook(RegisteredRoutesHook { routes -> routes.clearDefaultIgnoredRoutes() })
                }
            )
            config.routes.get("/users") { }
        }

        try {
            val document = jsonMapper.readTree(Unirest.get("http://localhost:${app.port()}/openapi").asString().body)
            val paths = document.path("paths")

            assertThat(paths.has("/openapi")).isTrue()
            assertThat(paths.has("/users")).isTrue()
        } finally {
            app.stop()
        }
    }

    @Test
    fun `ignores configured path prefixes without matching adjacent paths`() {
        val app = Javalin.start { config ->
            config.jetty.port = 0
            config.registerPlugin(
                OpenApiPlugin {
                    it.withHook(RegisteredRoutesHook { routes -> routes.withIgnoredPathPrefix("/assets/*") })
                }
            )
            config.routes.get("/assets/logo.svg") { }
            config.routes.get("/assets-admin") { }
        }

        try {
            val document = jsonMapper.readTree(Unirest.get("http://localhost:${app.port()}/openapi").asString().body)
            val paths = document.path("paths")

            assertThat(paths.has("/assets/logo.svg")).isFalse()
            assertThat(paths.has("/assets-admin")).isTrue()
        } finally {
            app.stop()
        }
    }

    @Test
    fun `enriches a route from OpenApiMetadata, reusing the operation builder and schema engine`() {
        val app = Javalin.start { config ->
            config.jetty.port = 0
            config.registerPlugin(OpenApiPlugin { it.withHook(RegisteredRoutesHook()) })
            config.routes.addEndpoint(
                Endpoint.create(HandlerType.GET, "/users/{id}")
                    .addMetadata(OpenApiMetadata {
                        summary("Get a user")
                        responses {
                            response("200") {
                                description("The user")
                                content { mediaType("application/json") { schema(User::class.java) } }
                            }
                        }
                    })
                    .handler { }
            )
        }

        try {
            val document = jsonMapper.readTree(Unirest.get("http://localhost:${app.port()}/openapi").asString().body)
            val get = document.path("paths").path("/users/{id}").path("get")

            assertThat(get.path("summary").asText()).isEqualTo("Get a user")
            assertThat(get.path("parameters")[0].path("name").asText()).isEqualTo("id")

            val schema = get.path("responses").path("200").path("content").path("application/json").path("schema")
            assertThat(schema.path($$"$ref").asText()).isEqualTo("#/components/schemas/User")

            val user = document.path("components").path("schemas").path("User")
            assertThat(user.path("type").asText()).isEqualTo("object")
            assertThat(user.path("properties").fieldNames().asSequence().toList()).containsExactlyInAnyOrder("id", "name")
        } finally {
            app.stop()
        }
    }

    @Test
    fun `leaves an existing operation untouched when no runtime metadata is present`() {
        val app = Javalin.start { config ->
            config.jetty.port = 0
            config.registerPlugin(
                OpenApiPlugin { plugin ->
                    plugin.withHook { context ->
                        context.builder.path("/users/{id}").operation("get") {
                            summary("Find a user")
                            parameters {
                                parameter("id", "path", required = true) { type("integer") }
                            }
                            responses {
                                response("200") { description("A user") }
                            }
                        }
                    }
                    plugin.withHook(RegisteredRoutesHook())
                }
            )
            config.routes.get("/users/{id}") { }
        }

        try {
            val document = jsonMapper.readTree(Unirest.get("http://localhost:${app.port()}/openapi").asString().body)
            val operation = document.path("paths").path("/users/{id}").path("get")

            assertThat(operation.path("summary").asText()).isEqualTo("Find a user")
            assertThat(operation.path("parameters")[0].path("schema").path("type").asText()).isEqualTo("integer")
            assertThat(operation.path("responses").path("200").path("description").asText()).isEqualTo("A user")
        } finally {
            app.stop()
        }
    }
}
