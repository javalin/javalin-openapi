package io.javalin.openapi.plugin.swagger

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.openapi.OpenApiPluginRouteHandler
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.http.MimeTypes
import java.io.InputStream

internal class SwaggerWebJarHandler(
    private val swaggerWebJarPath: String,
    private val classLoader: ClassLoader = SwaggerWebJarHandler::class.java.classLoader,
) : Handler, OpenApiPluginRouteHandler {

    override fun handle(context: Context) {
        val resourceRootPath = "META-INF/resources$swaggerWebJarPath"

        val requestedResource = context.path()
            .replaceFirst(context.contextPath(), "")
            .replaceFirst(swaggerWebJarPath, "")

        val resource: InputStream? = classLoader.getResourceAsStream(resourceRootPath + requestedResource)

        if (resource == null) {
            context.status(HttpStatus.NOT_FOUND_404)
            return
        }

        context.result(resource)
        context.res().characterEncoding = "UTF-8"

        // Swagger returns non-standard assets such as .js.map, which Jetty does not otherwise recognize.
        MimeTypes.DEFAULTS.getMimeByExtension(context.path())?.let {
            context.contentType(it)
        }
    }

}
