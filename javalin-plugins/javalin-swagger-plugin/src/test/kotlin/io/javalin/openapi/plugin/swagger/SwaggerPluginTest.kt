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
    fun `should not fail if second swagger plugin is registered`(){
        val swaggerConfiguration = SwaggerConfiguration();
        val otherConfiguration = ExampleSwaggerPlugin();
        Javalin.create{
            it.plugins.register(SwaggerPlugin(swaggerConfiguration))
            it.plugins.register(otherConfiguration)
        }
            .start(8080)
            .use{
                val javalinHost = "http://localhost:8080"
                val webjarJsRoute = "/webjars/swagger-ui/${swaggerConfiguration.version}/swagger-ui-bundle.js"

                val response = Unirest.get("$javalinHost/swagger")
                    .asString()
                    .body

                assertThat(response).contains("""src="$webjarJsRoute"""")
                assertThat(response).contains("""url: '/openapi?v=test'""")
                assertThat(response).doesNotContain("""url: '/example-docs?v=test'""")

                val resourceResponse = Unirest.get("$javalinHost$webjarJsRoute")
                    .asString()
                    .body

                assertThat(resourceResponse).isNotBlank

                val otherResponse = Unirest.get("$javalinHost/example-ui")
                    .asString()
                    .body

                assertThat(otherResponse).contains("""url: '/example-docs?v=test'""")
                assertThat(otherResponse).doesNotContain("""url: '/openapi?v=test'""")
            }
    }

    @Test
    fun `should not fail if second swagger plugin is registered with routes`(){
        val swaggerConfiguration = SwaggerConfiguration();
        val otherConfiguration = ExampleSwaggerPlugin();
        Javalin.create{
            it.plugins.register(SwaggerPlugin(swaggerConfiguration))
            it.plugins.register(otherConfiguration)
        }.get("/some/route/"){ ctx -> ctx.result("Hello World")}
            .start(8080)
            .use{
                val javalinHost = "http://localhost:8080"

                val webjarCssRoute = "/webjars/swagger-ui/${swaggerConfiguration.version}/swagger-ui.css"
                val webjarJsRoute = "/webjars/swagger-ui/${swaggerConfiguration.version}/swagger-ui-bundle.js"
                val webjarJsStandaloneRoute = "/webjars/swagger-ui/${swaggerConfiguration.version}/swagger-ui-standalone-preset.js"

                val response = Unirest.get("$javalinHost/swagger")
                    .asString()
                    .body

                assertThat(response).contains("""href="$webjarCssRoute"""")
                assertThat(response).contains("""src="$webjarJsRoute"""")
                assertThat(response).contains("""src="$webjarJsStandaloneRoute"""")
                assertThat(response).contains("""url: '/openapi?v=test'""")
                assertThat(response).doesNotContain("""url: '/example-docs?v=test'""")

                var resourceResponse = Unirest.get("$javalinHost$webjarCssRoute")
                    .asString()
                    .body

                assertThat(resourceResponse).isNotBlank

                resourceResponse = Unirest.get("$javalinHost$webjarJsRoute")
                    .asString()
                    .body

                assertThat(resourceResponse).isNotBlank

                resourceResponse = Unirest.get("$javalinHost$webjarJsStandaloneRoute")
                    .asString()
                    .body

                assertThat(resourceResponse).isNotBlank

                val otherResponse = Unirest.get("$javalinHost/example-ui")
                    .asString()
                    .body

                assertThat(otherResponse).contains("""url: '/example-docs?v=test'""")
                assertThat(otherResponse).doesNotContain("""url: '/openapi?v=test'""")
            }
    }

    class ExampleSwaggerPlugin : SwaggerPlugin(
        SwaggerConfiguration().apply {
            this.documentationPath = "/example-docs"
            this.uiPath = "/example-ui"
        }
    )

}
