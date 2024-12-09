package io.javalin.openapi.plugin.test

import io.javalin.Javalin
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.openapi.plugin.swagger.SwaggerPlugin

@OpenApi(
    description = "Test description",
    summary = "Test summary",
    tags = ["test-tag"],
    methods = [HttpMethod.GET],
    path = "/"
)
fun main() {
    Javalin.createAndStart { config ->
        config.registerPlugin(
            OpenApiPlugin {
                it.documentationPath = "/openapi"
            }
        )

        config.registerPlugin(
            SwaggerPlugin {
                it.uiPath = "/swagger"
                it.documentationPath = "/openapi"
            }
        )
    }
}