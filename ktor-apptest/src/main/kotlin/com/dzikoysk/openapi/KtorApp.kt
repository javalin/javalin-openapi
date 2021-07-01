package com.dzikoysk.openapi

import com.dzikoysk.openapi.annotations.HttpMethod
import com.dzikoysk.openapi.annotations.OpenApi
import com.dzikoysk.openapi.ktor.OpenApiFeature
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

private const val ROUTE = "/main"

fun main() {
    embeddedServer(Netty, port = 80) {
        install(DefaultHeaders)
        install(CallLogging)
        install(OpenApiFeature) {
            documentationPath = "/swagger-docs"
        }
        routing {
            get(ROUTE) {
                handleMain(call)
            }
        }
    }.start(wait = true)
}

@OpenApi(
    path = ROUTE,
    operationId = "main",
    methods = [HttpMethod.GET],
    summary = "Default application",
    description = "Random description",
    tags = ["General"],
)
private suspend fun handleMain(call: ApplicationCall) {
    call.respondText { "Hello Ktor" }
}