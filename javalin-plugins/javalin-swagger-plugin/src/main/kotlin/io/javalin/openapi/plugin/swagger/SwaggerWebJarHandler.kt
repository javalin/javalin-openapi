package io.javalin.openapi.plugin.swagger

import io.javalin.http.Context
import io.javalin.http.Handler
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.http.MimeTypes

internal class SwaggerWebJarHandler(private val swaggerWebJarPath: String) : Handler {

    override fun handle(context: Context) {
        val resource = SwaggerPlugin::class.java.getResourceAsStream("/META-INF/resources" + swaggerWebJarPath + context.path().replaceFirst(swaggerWebJarPath, ""))

        if (resource == null) {
            context.status(HttpStatus.NOT_FOUND_404)
            return
        }

        context.result(resource)
        context.res().characterEncoding = "UTF-8"

        MimeTypes.getDefaultMimeByExtension(context.path())?.let { // Swagger returns various non-standard assets like .js.map that are not recognized
            context.contentType(it)
        }
    }

}