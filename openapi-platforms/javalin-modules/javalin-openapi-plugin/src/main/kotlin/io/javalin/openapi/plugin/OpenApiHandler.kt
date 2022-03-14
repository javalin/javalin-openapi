package io.javalin.openapi.plugin

import io.javalin.core.util.Header
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.annotations.ContentType

internal class OpenApiHandler(private val documentation: String) : Handler {

    override fun handle(context: Context) {
        context
            .header(Header.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
            .header(Header.ACCESS_CONTROL_ALLOW_METHODS, "GET")
            .contentType(ContentType.JSON)
            .result(documentation)
    }

}