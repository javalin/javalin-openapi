package io.javalin.openapi.ksp.example

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.OpenApiStatus
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.openapi.plugin.redoc.ReDocPlugin
import io.javalin.openapi.plugin.swagger.SwaggerPlugin

data class Account(
    val id: String,
    val age: Int,
    val roles: List<String>,
)

class AccountHandler : Handler {
    @OpenApi(
        path = "/account",
        methods = [HttpMethod.GET],
        summary = "Get the current account",
        operationId = "getAccount",
        responses = [
            OpenApiResponse(
                status = OpenApiStatus.OK,
                content = [OpenApiContent(from = Account::class)],
            )
        ],
    )
    override fun handle(ctx: Context) {
        ctx.json(Account(id = "u-1", age = 30, roles = listOf("admin")))
    }
}

fun main() {
    Javalin.start { config ->
        config.registerPlugin(OpenApiPlugin {})
        config.registerPlugin(SwaggerPlugin {})
        config.registerPlugin(ReDocPlugin {})

        config.routes.get("/account", AccountHandler())
    }

    println("OpenAPI document: http://localhost:8080/openapi")
    println("Swagger UI:       http://localhost:8080/swagger")
    println("ReDoc:            http://localhost:8080/redoc")
}
