package com.dzikoysk.openapi.javalin.redoc

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginLifecycleInit

class ReDocConfiguration {

    var title = "OpenApi documentation"
    var version = "2.0.0-rc.53"
    var uiPath = "/redoc"
    var webJarPath = "/webjars/redoc"
    var documentationPath = "/openapi"

}

class ReDocPlugin(private val configuration: ReDocConfiguration) : Plugin, PluginLifecycleInit {

    override fun init(app: Javalin) {}

    override fun apply(app: Javalin) {
        app
            .get(configuration.uiPath, ReDocHandler(configuration.title, configuration.documentationPath, configuration.version))
            .get("${configuration.webJarPath}/*", ReDocWebJarHandler(configuration.webJarPath))
    }

}