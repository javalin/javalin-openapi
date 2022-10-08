package io.javalin.openapi.plugin.redoc

import io.javalin.Javalin
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginLifecycleInit
import io.javalin.security.RouteRole

class ReDocConfiguration {
    var title = "OpenApi documentation"
    var version = "2.0.0-rc.70"
    var uiPath = "/redoc"
    var webJarPath = "/webjars/redoc"
    var documentationPath = "/openapi"
    var roles: Array<RouteRole> = emptyArray()
}

class ReDocPlugin @JvmOverloads constructor(private val configuration: ReDocConfiguration = ReDocConfiguration()) : Plugin, PluginLifecycleInit {

    override fun init(app: Javalin) {}

    override fun apply(app: Javalin) {
        app
            .get(configuration.uiPath, ReDocHandler(configuration.title, configuration.documentationPath, configuration.version), *configuration.roles)
            .get("${configuration.webJarPath}/*", ReDocWebJarHandler(configuration.webJarPath), *configuration.roles)
    }

}