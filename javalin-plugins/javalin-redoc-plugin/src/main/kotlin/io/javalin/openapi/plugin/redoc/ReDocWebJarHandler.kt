package io.javalin.openapi.plugin.redoc

import io.javalin.http.Context
import io.javalin.http.Handler
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.http.MimeTypes

internal class ReDocWebJarHandler(private val redocWebJarPath: String) : Handler {

    override fun handle(context: Context) {
        val resource = ReDocPlugin::class.java.getResourceAsStream("/META-INF/resources" + redocWebJarPath + context.path().replaceFirst(context.contextPath(), "").replaceFirst(redocWebJarPath, ""))

        if (resource == null) {
            context.status(HttpStatus.NOT_FOUND_404)
            return
        }

        context.res().characterEncoding = "UTF-8"
        context.result(resource)

        MimeTypes.getDefaultMimeByExtension(context.path())?.let {
            context.contentType(it)
        }
    }

}