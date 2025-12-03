package io.javalin.openapi.plugin.swagger

import io.javalin.Javalin
import io.javalin.openapi.plugin.swagger.specification.JavalinBehindProxy
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SwaggerPluginTest {
    @Test
    fun `should properly host swagger ui`() {
        val app = Javalin.createAndStart {
            it.jetty.defaultPort = 0
            it.registerPlugin(SwaggerPlugin())
        }

        try {
            val response = Unirest.get("http://localhost:${app.port()}/swagger")
                .asString()
                .body

            assertThat(response).contains("""href="/webjars/swagger-ui/${SwaggerConfiguration().version}/swagger-ui.css"""")
            assertThat(response).contains("""src="/webjars/swagger-ui/${SwaggerConfiguration().version}/swagger-ui-bundle.js"""")
            assertThat(response).contains("""src="/webjars/swagger-ui/${SwaggerConfiguration().version}/swagger-ui-standalone-preset.js"""")
            assertThat(response).contains("""url: '/openapi?v=test'""")
        } finally {
            app.stop()
        }
    }

    @Test
    fun `should support custom base path`() {
        JavalinBehindProxy(
            javalinSupplier = { Javalin.create { it.registerPlugin(SwaggerPlugin { it.basePath = "/custom" }) } },
            basePath = "/custom"
        ).use {
            val response = Unirest.get("http://localhost:${it.proxyPort()}/custom/swagger")
                .asString()
                .body

            assertThat(response).contains("""href="/custom/webjars/swagger-ui/${SwaggerConfiguration().version}/swagger-ui.css"""")
            assertThat(response).contains("""src="/custom/webjars/swagger-ui/${SwaggerConfiguration().version}/swagger-ui-bundle.js"""")
            assertThat(response).contains("""src="/custom/webjars/swagger-ui/${SwaggerConfiguration().version}/swagger-ui-standalone-preset.js"""")
            assertThat(response).contains("""url: '/custom/openapi?v=test'""")
        }
    }

    @Test
    fun `should have custom version, css and js injected`() {
        val app = Javalin.createAndStart {
            it.jetty.defaultPort = 0
            it.registerPlugin(SwaggerPlugin { swagger ->
                swagger
                    .injectStylesheet("/swagger.css")
                    .injectStylesheet("/swagger-the-print.css", "print")
                    .injectJavaScript("/script.js")
                    .injectCustomVersion("custom", "/openapi.yaml")
            })
        }

        try {
            val response = Unirest.get("http://localhost:${app.port()}/swagger")
                .asString()
                .body

            assertThat(response).contains("""link href='/swagger.css' rel='stylesheet' media='screen' type='text/css'""")
            assertThat(response).contains("""link href='/swagger-the-print.css' rel='stylesheet' media='print' type='text/css'""")
            assertThat(response).contains("""script src='/script.js' type='text/javascript'""")
            assertThat(response).contains("{ name: 'custom', url: '/openapi.yaml' }")
        } finally {
            app.stop()
        }
    }

    @Test
    fun `should not fail if second swagger plugin is registered`() {
        val app = Javalin.createAndStart {
            it.jetty.defaultPort = 0
            it.registerPlugin(SwaggerPlugin())
            it.registerPlugin(SwaggerPlugin { swagger ->
                swagger.documentationPath = "/example-docs"
                swagger.uiPath = "/example-ui"
            })
        }

        try {
            val javalinHost = "http://localhost:${app.port()}"
            val webjarJsRoute = "/webjars/swagger-ui/${SwaggerConfiguration().version}/swagger-ui-bundle.js"

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
        } finally {
            app.stop()
        }
    }

    @Test
    fun `should not fail if second swagger plugin is registered with routes`(){
        val app = Javalin.createAndStart {
            it.jetty.defaultPort = 0
            it.registerPlugin(SwaggerPlugin())
            it.registerPlugin(SwaggerPlugin { swagger ->
                swagger.documentationPath = "/example-docs"
                swagger.uiPath = "/example-ui"
            })
            it.router.mount { cfg ->
                cfg.get("/some/route/") { ctx -> ctx.result("Hello World") }
            }
        }

        try {
            val javalinHost = "http://localhost:${app.port()}"

            val webjarCssRoute = "/webjars/swagger-ui/${SwaggerConfiguration().version}/swagger-ui.css"
            val webjarJsRoute = "/webjars/swagger-ui/${SwaggerConfiguration().version}/swagger-ui-bundle.js"
            val webjarJsStandaloneRoute = "/webjars/swagger-ui/${SwaggerConfiguration().version}/swagger-ui-standalone-preset.js"

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
        } finally {
            app.stop()
        }
    }

}
