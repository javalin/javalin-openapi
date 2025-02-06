import io.javalin.Javalin
import io.javalin.openapi.OpenApi
import io.javalin.openapi.plugin.OpenApiPlugin
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpenApiPluginTest {

    @OpenApi(
        path = "/test",
    )
    private object OpenApiTest

    @Test
    fun `should support schema modifications in definition configuration`() {
        val app =
            Javalin.createAndStart { config ->
                config.jetty.defaultPort = 0

                config.registerPlugin(
                    OpenApiPlugin { openApiConfig ->
                        openApiConfig.withDefinitionConfiguration { _, def ->
                            def.withInfo {
                                it.title = "My API"
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
        val app = Javalin.createAndStart { config ->
            config.jetty.defaultPort = 0

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

}