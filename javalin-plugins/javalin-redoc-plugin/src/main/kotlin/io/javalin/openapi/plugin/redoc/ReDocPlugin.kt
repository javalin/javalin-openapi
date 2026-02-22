@file:Suppress("MemberVisibilityCanBePrivate")

package io.javalin.openapi.plugin.redoc

import io.javalin.config.JavalinState
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.http.HandlerType.GET
import io.javalin.plugin.Plugin
import io.javalin.router.Endpoint
import io.javalin.security.Roles
import io.javalin.security.RouteRole
import java.util.function.Consumer

/** Configure ReDoc UI plugin */
class ReDocConfiguration @JvmOverloads constructor(
    /** Page title */
    @JvmField var title: String = "OpenApi documentation",
    /** ReDoc route */
    @JvmField var uiPath: String = "/redoc",
    /** Roles permitted to access ReDoc UI */
    @JvmField var roles: Array<RouteRole> = emptyArray(),
    /** Location of OpenApi documentation */
    @JvmField var documentationPath: String = "/openapi",
    /** Custom base path if Javalin is running behind reverse proxy */
    @JvmField var basePath: String? = null,
    /** ReDoc Bundle version */
    @JvmField var version: String = "2.5.0",
    /** ReDoc WebJar route */
    @JvmField var webJarPath: String = "/webjars/redoc",
) {

    /** Set page title */
    fun withTitle(title: String): ReDocConfiguration = also { this.title = title }

    /** Set the ReDoc route */
    fun withUiPath(path: String): ReDocConfiguration = also { uiPath = path }

    /** Set roles permitted to access ReDoc UI */
    fun withRoles(vararg roles: RouteRole): ReDocConfiguration = also { this.roles = arrayOf(*roles) }

    /** Set the location of OpenApi documentation */
    fun withDocumentationPath(path: String): ReDocConfiguration = also { documentationPath = path }

    /** Set custom base path if Javalin is running behind reverse proxy */
    fun withBasePath(path: String): ReDocConfiguration = also { basePath = path }

    /** Set ReDoc Bundle version */
    fun withVersion(version: String): ReDocConfiguration = also { this.version = version }

    /** Set ReDoc WebJar route */
    fun withWebJarPath(path: String): ReDocConfiguration = also { webJarPath = path }
}

internal class ReDocEndpoint(
    method: HandlerType,
    path: String,
    roles: Set<RouteRole>,
    handler: Handler
) : Endpoint(
    method = method,
    path = path,
    metadata = setOf(Roles(roles)),
    handler = handler
)

open class ReDocPlugin @JvmOverloads constructor(
    userConfig: Consumer<ReDocConfiguration> = Consumer {},
) : Plugin<ReDocConfiguration>(
    userConfig = userConfig,
    defaultConfig = ReDocConfiguration(),
) {

    override fun repeatable(): Boolean = true

    override fun onStart(state: JavalinState) {
        val reDocHandler = ReDocHandler(
            title = pluginConfig.title,
            documentationPath = pluginConfig.documentationPath,
            version = pluginConfig.version,
            routingPath = state.router.contextPath,
            basePath = pluginConfig.basePath
        )

        val reDocEndpoint = ReDocEndpoint(
            method = GET,
            path = pluginConfig.uiPath,
            roles = pluginConfig.roles.toSet(),
            handler = reDocHandler
        )

        state.routes.let { router ->
            router.addEndpoint(reDocEndpoint)

            /** Register webjar handler if and only if there isn't already a [ReDocWebJarHandler] at configured route */
            state.internalRouter
                .findHttpHandlerEntries(GET, "${pluginConfig.webJarPath}/*")
                .takeIf { routes -> routes.noneMatch { it.endpoint is ReDocEndpoint } }
                ?.run {
                    val webJarHandler = ReDocWebJarHandler(
                        redocWebJarPath = pluginConfig.webJarPath
                    )
                    router.addEndpoint(
                        ReDocEndpoint(
                            method = GET,
                            path = "${pluginConfig.webJarPath}/*",
                            roles = pluginConfig.roles.toSet(),
                            handler = webJarHandler
                        )
                    )
                }
        }
    }

}
