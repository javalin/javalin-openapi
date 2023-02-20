package io.javalin.openapi.plugin.swagger

import io.javalin.Javalin
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

        /* Better even would be to check here if the path is already registered in javalin */

        val swaggerWebJarHandler = SwaggerWebJarHandler(
            swaggerWebJarPath = configuration.webJarPath
        )

        app
            .get(configuration.uiPath, swaggerHandler, *configuration.roles)
        try {
            app.get("${configuration.webJarPath}/*", swaggerWebJarHandler, *configuration.roles)
        }catch(ex: java.lang.IllegalArgumentException){
            /* If there is any other exception than the one that this GET handler for this webJarPath already exists, we care.
            * Otherwise we can ignore it, as the main goal -- to serve the webjar -- is already achieved by some other configuration.
            */
            if(ex.message?.contains("type='GET'") != true || ex.message?.contains("path='${configuration.webJarPath}") != true){
                throw ex
            }
        }
    }

}
