package io.javalin.openapi.plugin.redoc

import io.javalin.Javalin
import io.javalin.openapi.plugin.redoc.specification.JavalinBehindProxy
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RedocPluginTest {

    @Test
    fun `should properly host redoc ui`() {
        val app = Javalin.create { it.registerPlugin(ReDocPlugin()) }.start(0)

        try {
            val response = Unirest.get("http://localhost:${app.port()}/redoc")
                .asString()
                .body

            assertThat(response).contains("""src="/webjars/redoc/${ReDocConfiguration().version}/bundles/redoc.standalone.js"""")
            assertThat(response).contains("""'/openapi'""")
        } finally {
            app.stop()
        }
    }

    @Test
    fun `should support custom base path`() {
        JavalinBehindProxy(
            javalinSupplier = { Javalin.create { it.registerPlugin(ReDocPlugin { redoc -> redoc.basePath = "/custom" }) } },
            basePath = "/custom"
        ).use {
            val response = Unirest.get("http://localhost:${it.proxyPort()}/custom/redoc")
                .asString()
                .body

            assertThat(response).contains("""src="/custom/webjars/redoc/${ReDocConfiguration().version}/bundles/redoc.standalone.js"""")
            assertThat(response).contains("""'/custom/openapi'""")
        }
    }

}