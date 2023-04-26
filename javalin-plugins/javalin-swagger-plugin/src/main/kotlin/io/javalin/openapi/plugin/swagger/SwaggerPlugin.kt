package io.javalin.openapi.plugin.swagger

import io.javalin.Javalin
import io.javalin.http.HandlerType
import io.javalin.http.servlet.getBasicAuthCredentials
import io.javalin.plugin.Plugin
import io.javalin.security.RouteRole

class SwaggerConfiguration {
    /** Page title **/
    var title = "OpenApi documentation"
    /* Swagger UI route */
    var uiPath = "/swagger"
    /** Roles eligible to connect to Swagger routes */
    var roles: Array<RouteRole> = emptyArray()
    /** Location of OpenApi documentation */
    var documentationPath = "/openapi"
    /** Specify custom base path if Javalin is running behind reverse proxy */
    var basePath: String? = null
    /** Swagger UI Bundle version */
    var version = "3.52.5"
    /** Swagger UI Bundler webjar location */
    var webJarPath = "/webjars/swagger-ui"
    /** Specification validator */
    var validatorUrl: String? = "https://validator.swagger.io/validator"
}

open class SwaggerPlugin @JvmOverloads constructor(private val configuration: SwaggerConfiguration = SwaggerConfiguration()) : Plugin {

    override fun apply(app: Javalin) {
        val swaggerHandler = SwaggerHandler(
            title = configuration.title,
            documentationPath = configuration.documentationPath,
            swaggerVersion = configuration.version,
            validatorUrl = configuration.validatorUrl,
            routingPath = app.cfg.routing.contextPath,
            basePath = configuration.basePath
        )
        /** Register handler for swagger ui */
        app.get(configuration.uiPath, swaggerHandler, *configuration.roles)

        /** Register webjar handler if and only if there isn't already a [SwaggerWebJarHandler] at configured route */
        app.javalinServlet().matcher
            .findEntries(HandlerType.GET, "${configuration.webJarPath}/*")
            .takeIf { routes -> routes.none { it.handler is SwaggerWebJarHandler } }
            ?.run {
                val swaggerWebJarHandler = SwaggerWebJarHandler(
                    swaggerWebJarPath = configuration.webJarPath
                )
                app.get("${configuration.webJarPath}/*", swaggerWebJarHandler, *configuration.roles)
            }
    }

}
