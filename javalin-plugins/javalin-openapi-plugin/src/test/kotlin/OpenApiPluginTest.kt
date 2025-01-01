import io.javalin.Javalin
import io.javalin.openapi.OpenApi
import io.javalin.openapi.plugin.OpenApiPlugin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class OpenApiPluginTest {

    @OpenApi(
        path = "/test",
    )
    private object OpenApiTest

    @Test
    fun `should support schema modifications in definition configuration`() {
        assertDoesNotThrow {
            Javalin.create { config ->
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
        }
    }

    @Test
    fun `should support empty definition configuration`() {
        assertDoesNotThrow {
            Javalin.create { config ->
                config.registerPlugin(
                    OpenApiPlugin {
                        it.withDefinitionConfiguration { _, _ ->
                            /* do nothing */
                        }
                    }
                )
            }
        }
    }

}