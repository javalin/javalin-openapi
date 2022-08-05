package io.javalin.openapi.plugin.redoc

import io.javalin.http.Context
import io.javalin.http.Handler
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.http.MimeTypes

internal class ReDocWebJarHandler(private val redocWebJarPath: String) : Handler {

    override fun handle(context: Context) {
        val resource = ReDocPlugin::class.java.getResourceAsStream("/META-INF/resources" + redocWebJarPath + context.path().replaceFirst(redocWebJarPath, ""))

        if (resource == null) {
            context.status(HttpStatus.NOT_FOUND_404)
            return
        }

        context.result(resource)
            .contentType(MimeTypes.getDefaultMimeByExtension(context.path()))
            .res().characterEncoding = "UTF-8"
    }

}