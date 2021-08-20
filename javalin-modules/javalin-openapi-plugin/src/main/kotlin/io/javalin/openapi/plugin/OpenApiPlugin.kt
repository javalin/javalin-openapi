package io.javalin.openapi.plugin

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginLifecycleInit
import org.slf4j.LoggerFactory
import java.util.function.Function

class OpenApiConfiguration {

    var title = "OpenApi Title"
    var description = "OpenApi Description"
    var version = "OpenApi Version"
    var documentationPath = "/openapi"
    var documentProcessor: Function<String, String> = Function.identity()

}

class OpenApiPlugin(private val configuration: OpenApiConfiguration) : Plugin, PluginLifecycleInit {

    private var documentation: String? = null
    private val logger = LoggerFactory.getLogger(OpenApiPlugin::class.java)

    override fun init(app: Javalin) {
        this.documentation = readResource("/openapi.json")
            ?.replaceFirst("{openapi.title}", configuration.title)
            ?.replaceFirst("{openapi.description}", configuration.description)
            ?.replaceFirst("{openapi.version}", configuration.version)
            ?.let { configuration.documentProcessor.apply(it) }
    }

    private fun readResource(path: String): String? =
        OpenApiPlugin::class.java.getResource(path)?.readText()

    override fun apply(app: Javalin) {
        if (documentation == null) {
            logger.warn("OpenApi documentation not found")
            return
        }

        app.get(configuration.documentationPath, OpenApiHandler(documentation!!))
    }

}