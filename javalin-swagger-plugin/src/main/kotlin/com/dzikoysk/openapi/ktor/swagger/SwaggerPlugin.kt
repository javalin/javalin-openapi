package com.dzikoysk.openapi.ktor.swagger

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginLifecycleInit

class SwaggerConfiguration {

    var title = "OpenApi documentation"
    var version = "3.50.0"
    var uiPath = "/swagger"
    var webJarPath = "/webjars/swagger-ui"
    var documentationPath = "/openapi"

}

class SwaggerPlugin(private val configuration: SwaggerConfiguration) : Plugin, PluginLifecycleInit {

    override fun init(app: Javalin) {
    }

    private fun readResource(path: String): String? =
        SwaggerPlugin::class.java.getResource(path)?.readText()

    override fun apply(app: Javalin) {
        app
            .get(configuration.uiPath, SwaggerHandler(configuration.title, configuration.documentationPath, configuration.version))
            .get("${configuration.webJarPath}/*", SwaggerWebJarHandler(configuration.webJarPath))
    }

}