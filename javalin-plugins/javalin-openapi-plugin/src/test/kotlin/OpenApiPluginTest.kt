import io.javalin.Javalin
import io.javalin.openapi.OpenApi
import io.javalin.openapi.dynamic.ReflectionSchemaContext
import io.javalin.openapi.experimental.processor.shared.jsonMapper
import io.javalin.openapi.plugin.OpenApiPlugin
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpenApiPluginTest {

    @OpenApi(
        path = "/test",
    )
    private object OpenApiTest

    private data class DefinitionConfigurationUser(val id: String)

    @Test
    fun `should support schema modifications in definition configuration`() {
        val app =
            Javalin.start { config ->
                config.jetty.port = 0

                config.registerPlugin(
                    OpenApiPlugin { openApiConfig ->
                        openApiConfig.withDefinitionConfiguration { _, builder ->
                            builder.info {
                                it.title("My API")
                            }
                        }
                    }
                )
            }

        try {
            val response = Unirest.get("http://localhost:${app.port()}/openapi")
                .asString()
                .body

            assertThat(response).contains(""""title" : "My API"""")
        } finally {
            app.stop()
        }
    }

    @Test
    fun `should support empty definition configuration`() {
        val app = Javalin.start { config ->
            config.jetty.port = 0

            config.registerPlugin(
                OpenApiPlugin {
                    it.withDefinitionConfiguration { _, _ ->
                        /* do nothing */
                    }
                }
            )
        }

        try {
            val response = Unirest.get("http://localhost:${app.port()}/openapi")
                .asString()
                .body

            assertThat(response).contains(""""title" : """"")
        } finally {
            app.stop()
        }
    }

    @Test
    fun `should support explicit schema reference resolution in definition configuration`() {
        val schemaContext = ReflectionSchemaContext()

        val app = Javalin.start { config ->
            config.jetty.port = 0

            config.registerPlugin(
                OpenApiPlugin {
                    it.withDefinitionConfiguration { _, builder ->
                        builder.path("/runtime-user").operation("get") {
                            responses {
                                response("200") {
                                    description("OK")
                                    content {
                                        mediaType("application/json") {
                                            schema(schemaContext.inlineSchema(DefinitionConfigurationUser::class.java))
                                        }
                                    }
                                }
                            }
                        }
                        builder.resolveComponentReferences { type -> schemaContext.componentSchema(type) }
                    }
                }
            )
        }

        try {
            val response = Unirest.get("http://localhost:${app.port()}/openapi")
                .asString()
                .body

            val document = jsonMapper.readTree(response)
            val schema = document
                .path("paths")
                .path("/runtime-user")
                .path("get")
                .path("responses")
                .path("200")
                .path("content")
                .path("application/json")
                .path("schema")

            assertThat(schema.path("\$ref").asText()).isEqualTo("#/components/schemas/DefinitionConfigurationUser")
            assertThat(document.path("components").path("schemas").has("DefinitionConfigurationUser")).isTrue()
            assertThat(
                document.path("components")
                    .path("schemas")
                    .path("DefinitionConfigurationUser")
                    .path("properties")
                    .has("id")
            ).isTrue()
        } finally {
            app.stop()
        }
    }

}
