package io.javalin.openapi.plugin.redoc

import io.javalin.Javalin
import io.javalin.openapi.plugin.redoc.specification.JavalinBehindProxy
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RedocPluginTest {

    @Test
    fun `should properly host redoc ui`() {
        val app = Javalin.createAndStart { it.registerPlugin(ReDocPlugin()) }

        try {
            val response = Unirest.get("http://localhost:8080/redoc")
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
            port = 8080,
            basePath = "/custom"
        ).use {
            val response = Unirest.get("http://localhost:8080/custom/redoc")
                .asString()
                .body

            assertThat(response).contains("""src="/custom/webjars/redoc/${ReDocConfiguration().version}/bundles/redoc.standalone.js"""")
            assertThat(response).contains("""'/custom/openapi'""")
        }
    }

}