package io.javalin.openapi.plugin.swagger

import io.javalin.Javalin
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginLifecycleInit
import io.javalin.security.RouteRole

class SwaggerConfiguration {
    var title = "OpenApi documentation"
    var version = "3.52.5"
    var uiPath = "/swagger"
    var webJarPath = "/webjars/swagger-ui"
    var documentationPath = "/openapi"
    var roles: Array<RouteRole> = emptyArray()
}

class SwaggerPlugin @JvmOverloads constructor(private val configuration: SwaggerConfiguration = SwaggerConfiguration()) : Plugin, PluginLifecycleInit {

    override fun init(app: Javalin) {}

    override fun apply(app: Javalin) {
        app
            .get(configuration.uiPath, SwaggerHandler(configuration.title, configuration.documentationPath, configuration.version), *configuration.roles)
            .get("${configuration.webJarPath}/*", SwaggerWebJarHandler(configuration.webJarPath), *configuration.roles)
    }

}