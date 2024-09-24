package io.javalin.openapi.plugin.redoc

import io.javalin.config.JavalinConfig
import io.javalin.plugin.Plugin
import io.javalin.security.RouteRole
import java.util.function.Consumer

class ReDocConfiguration {
    /** Page title */
    var title = "OpenApi documentation"
    /** ReDoc route */
    var uiPath = "/redoc"
    /* Roles permitted to access ReDoc UI */
    var roles: Array<RouteRole> = emptyArray()
    /** Location of OpenApi documentation */
    var documentationPath = "/openapi"
    /* Custom base path */
    var basePath: String? = null
    /** ReDoc Bundle version **/
    var version = "2.1.4"
    /** ReDoc WebJar route */
    var webJarPath = "/webjars/redoc"
}

open class ReDocPlugin @JvmOverloads constructor(userConfig: Consumer<ReDocConfiguration> = Consumer {}) : Plugin<ReDocConfiguration>(userConfig, ReDocConfiguration()) {

    override fun onStart(config: JavalinConfig) {
        val reDocHandler = ReDocHandler(
            title = pluginConfig.title,
            documentationPath = pluginConfig.documentationPath,
            version = pluginConfig.version,
            routingPath = config.router.contextPath,
            basePath = pluginConfig.basePath
        )

        val webJarHandler = ReDocWebJarHandler(
            redocWebJarPath = pluginConfig.webJarPath
        )

        config.router.mount { router ->
            router
                .get(pluginConfig.uiPath, reDocHandler, *pluginConfig.roles)
                .get("${pluginConfig.webJarPath}/*", webJarHandler, *pluginConfig.roles)
        }
    }

}