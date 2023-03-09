package io.javalin.openapi.plugin.swagger

import io.javalin.Javalin
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
    var customStylesheetFiles: ArrayList<String> = arrayListOf()
    /**  Custom JavaScript files to be injected into Swagger HTML */
    var customJavaScriptFiles: ArrayList<String> = arrayListOf()

    @JvmOverloads
    fun injectStylesheet(path: String, media: String = "screen") {
        customStylesheetFiles.add("<link href='${path}' rel='stylesheet' media='${media}' type='text/css' />")
    }

    @JvmOverloads
    fun injectJavaScript(path: String, type: String = "text/javascript") {
        customJavaScriptFiles.add("<script src='${path}' type='${type}' />")
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

        val swaggerWebJarHandler = SwaggerWebJarHandler(
            swaggerWebJarPath = configuration.webJarPath
        )

        app
            .get(configuration.uiPath, swaggerHandler, *configuration.roles)
            .get("${configuration.webJarPath}/*", swaggerWebJarHandler, *configuration.roles)
    }

}