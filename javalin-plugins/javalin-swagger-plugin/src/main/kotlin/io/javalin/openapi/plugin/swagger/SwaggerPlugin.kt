package io.javalin.openapi.plugin.swagger

import io.javalin.Javalin
import io.javalin.http.HandlerType
import io.javalin.plugin.Plugin
import io.javalin.security.RouteRole

class SwaggerConfiguration {
    /** Location of OpenApi documentation */
    var documentationPath = "/openapi"
    /* Swagger UI route */
    var uiPath = "/swagger"
    /** Roles eligible to connect to Swagger routes */
    var roles: Array<RouteRole> = emptyArray()
    /** Specify custom base path if Javalin is running behind reverse proxy */
    var basePath: String? = null

    // WebJar configuration
    /** Swagger UI Bundle version */
    var version = "3.52.5"
    /** Swagger UI Bundler webjar location */
    var webJarPath = "/webjars/swagger-ui"

    // Swagger UI bundle configuration
    // ~ https://swagger.io/docs/open-source-tools/swagger-ui/usage/configuration/ */
    /** Page title **/
    var title = "OpenApi documentation"
    /** Specification validator */
    var validatorUrl: String? = "https://validator.swagger.io/validator"
    /** Tags sorter algorithm expression. */
    var tagsSorter: String = "'alpha'"
    /** Operations sorter algorithm expression. */
    var operationsSorter: String = "'alpha'"
    /**  Custom CSS files to be injected into Swagger HTML */
    var customStylesheetFiles: MutableList<Pair<String, String>> = arrayListOf()
    /**  Custom JavaScript files to be injected into Swagger HTML */
    var customJavaScriptFiles: MutableList<Pair<String, String>> = arrayListOf()

    @JvmOverloads
    fun injectStylesheet(path: String, media: String = "screen"): SwaggerConfiguration = also {
        customStylesheetFiles.add(Pair(path, media));
    }

    @JvmOverloads
    fun injectJavaScript(path: String, type: String = "text/javascript"): SwaggerConfiguration = also  {
        customJavaScriptFiles.add(Pair(path, type))
    }
}

open class SwaggerPlugin @JvmOverloads constructor(private val configuration: SwaggerConfiguration = SwaggerConfiguration()) : Plugin {

    override fun apply(app: Javalin) {
        val swaggerHandler = SwaggerHandler(
            title = configuration.title,
            documentationPath = configuration.documentationPath,
            swaggerVersion = configuration.version,
            validatorUrl = configuration.validatorUrl,
            routingPath = app.cfg.routing.contextPath,
            basePath = configuration.basePath,
            tagsSorter = configuration.tagsSorter,
            operationsSorter = configuration.operationsSorter,
            customStylesheetFiles = configuration.customStylesheetFiles,
            customJavaScriptFiles = configuration.customJavaScriptFiles
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
