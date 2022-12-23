package io.javalin.openapi.plugin.redoc

import io.javalin.Javalin
import io.javalin.openapi.plugin.redoc.specification.JavalinBehindProxy
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RedocPluginTest {

    @Test
    fun `should properly host redoc ui`() {
        val reDocConfiguration = ReDocConfiguration()

        Javalin.create { it.plugins.register(ReDocPlugin(reDocConfiguration)) }
            .start(8080)
            .use {
                val response = Unirest.get("http://localhost:8080/redoc")
                    .asString()
                    .body

                assertThat(response).contains("""src="/webjars/redoc/${reDocConfiguration.version}/bundles/redoc.standalone.js"""")
                assertThat(response).contains("""'/openapi'""")
            }
    }

    @Test
    fun `should support custom base path`() {
        val reDocConfiguration = ReDocConfiguration().apply {
            basePath = "/custom"
        }

        JavalinBehindProxy(
            javalinSupplier = { Javalin.create { it.plugins.register(ReDocPlugin(reDocConfiguration)) } },
            port = 8080,
            basePath = "/custom"
        ).use {
            val response = Unirest.get("http://localhost:8080/custom/redoc")
                .asString()
                .body

            assertThat(response).contains("""src="/custom/webjars/redoc/${reDocConfiguration.version}/bundles/redoc.standalone.js"""")
            assertThat(response).contains("""'/custom/openapi'""")
        }
    }

}