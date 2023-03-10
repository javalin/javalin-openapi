package io.javalin.openapi.plugin.swagger

import io.javalin.Javalin
import io.javalin.openapi.plugin.swagger.specification.JavalinBehindProxy
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SwaggerPluginTest {

    @Test
    fun `should properly host swagger ui`() {
        val swaggerConfiguration = SwaggerConfiguration()

        Javalin.create { it.plugins.register(SwaggerPlugin(swaggerConfiguration)) }
            .start(8080)
            .use {
                val response = Unirest.get("http://localhost:8080/swagger")
                    .asString()
                    .body

                assertThat(response).contains("""href="/webjars/swagger-ui/${swaggerConfiguration.version}/swagger-ui.css"""")
                assertThat(response).contains("""src="/webjars/swagger-ui/${swaggerConfiguration.version}/swagger-ui-bundle.js"""")
                assertThat(response).contains("""src="/webjars/swagger-ui/${swaggerConfiguration.version}/swagger-ui-standalone-preset.js"""")
                assertThat(response).contains("""url: '/openapi?v=test'""")
            }
    }

    @Test
    fun `should support custom base path`() {
        val swaggerConfiguration = SwaggerConfiguration().apply {
            basePath = "/custom"
        }

        JavalinBehindProxy(
            javalinSupplier = { Javalin.create { it.plugins.register(SwaggerPlugin(swaggerConfiguration)) } },
            port = 8080,
            basePath = "/custom"
        ).use {
            val response = Unirest.get("http://localhost:8080/custom/swagger")
                .asString()
                .body

            assertThat(response).contains("""href="/custom/webjars/swagger-ui/${swaggerConfiguration.version}/swagger-ui.css"""")
            assertThat(response).contains("""src="/custom/webjars/swagger-ui/${swaggerConfiguration.version}/swagger-ui-bundle.js"""")
            assertThat(response).contains("""src="/custom/webjars/swagger-ui/${swaggerConfiguration.version}/swagger-ui-standalone-preset.js"""")
            assertThat(response).contains("""url: '/custom/openapi?v=test'""")
        }
    }

    @Test
    fun `should have custom css and js injected`() {
        val swaggerConfiguration = SwaggerConfiguration()
            .injectStylesheet("/swagger.css")
            .injectStylesheet("/swagger-the-print.css", "print")
            .injectJavaScript("/script.js")

        Javalin.create { it.plugins.register(SwaggerPlugin(swaggerConfiguration)) }
            .start(8080)
            .use {
                val response = Unirest.get("http://localhost:8080/swagger")
                    .asString()
                    .body

                assertThat(response).contains("""link href='/swagger.css' rel='stylesheet' media='screen' type='text/css'""")
                assertThat(response).contains("""link href='/swagger-the-print.css' rel='stylesheet' media='print' type='text/css'""")
                assertThat(response).contains("""script src='/script.js' type='text/javascript'""")
            }
    }

}