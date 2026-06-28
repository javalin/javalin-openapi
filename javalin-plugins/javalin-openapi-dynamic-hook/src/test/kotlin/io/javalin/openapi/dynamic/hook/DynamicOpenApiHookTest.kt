package io.javalin.openapi.dynamic.hook

import io.javalin.Javalin
import io.javalin.http.HandlerType
import io.javalin.openapi.experimental.processor.shared.jsonMapper
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.router.Endpoint
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DynamicOpenApiHookTest {

    @Test
    fun `autogenerates docs for all registered Javalin routes`() {
        val app = Javalin.start { config ->
            config.jetty.port = 0
            config.registerPlugin(OpenApiPlugin { it.withHook(RegisteredRoutesHook()) })
            config.routes.get("/users") { }
            config.routes.post("/users") { }
            config.routes.get("/users/{id}") { }
        }

        try {
            val body = Unirest.get("http://localhost:${app.port()}/openapi").asString().body
            val paths = jsonMapper.readTree(body).path("paths")

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
            // Path param skeleton is still auto-added, then the metadata is overlaid.
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
}
