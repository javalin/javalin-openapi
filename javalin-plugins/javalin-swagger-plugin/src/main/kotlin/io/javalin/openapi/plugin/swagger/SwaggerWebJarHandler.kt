package io.javalin.openapi.plugin.swagger

import io.javalin.http.Context
import io.javalin.http.Handler
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.http.MimeTypes
import java.io.InputStream

internal class SwaggerWebJarHandler(
    private val swaggerWebJarPath: String,
) : Handler {

    override fun handle(context: Context) {
        val resourceRootPath = "/META-INF/resources$swaggerWebJarPath"

        val requestedResource = context.path()
            .replaceFirst(context.contextPath(), "")
            .replaceFirst(swaggerWebJarPath, "")

        val resource: InputStream? = SwaggerPlugin::class.java.getResourceAsStream(resourceRootPath + requestedResource)

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