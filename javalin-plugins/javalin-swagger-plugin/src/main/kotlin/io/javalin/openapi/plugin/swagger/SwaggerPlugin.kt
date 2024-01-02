package io.javalin.openapi.plugin.swagger

import io.javalin.config.JavalinConfig
import io.javalin.http.HandlerType
import io.javalin.http.HandlerType.GET
import io.javalin.openapi.OpenApiLoader
import io.javalin.plugin.Plugin
import io.javalin.security.RouteRole
import java.util.function.Consumer

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

open class SwaggerPlugin @JvmOverloads constructor(userConfig: Consumer<SwaggerConfiguration> = Consumer {}) : Plugin<SwaggerConfiguration>(userConfig, SwaggerConfiguration()) {

    override fun onStart(config: JavalinConfig) {
        val versions = OpenApiLoader()
            .loadVersions()

        val swaggerHandler = SwaggerHandler(
            title = pluginConfig.title,
            documentationPath = pluginConfig.documentationPath,
            versions = versions,
            swaggerVersion = pluginConfig.version,
            validatorUrl = pluginConfig.validatorUrl,
            routingPath = config.router.contextPath,
            basePath = pluginConfig.basePath,
            tagsSorter = pluginConfig.tagsSorter,
            operationsSorter = pluginConfig.operationsSorter,
            customStylesheetFiles = pluginConfig.customStylesheetFiles,
            customJavaScriptFiles = pluginConfig.customJavaScriptFiles
        )

        val swaggerEndpoint = SwaggerEndpoint(
            method = HandlerType.GET,
            path = pluginConfig.uiPath,
            roles = pluginConfig.roles.toSet(),
            handler = swaggerHandler
        )

        config.router.mount { router ->
            /** Register handler for swagger ui */
            router.addEndpoint(swaggerEndpoint)

            /** Register webjar handler if and only if there isn't already a [SwaggerWebJarHandler] at configured route */
            config.pvt.internalRouter
                .findHttpHandlerEntries(HandlerType.GET, "${pluginConfig.webJarPath}/*")
                .takeIf { routes -> routes.noneMatch { it.endpoint is SwaggerEndpoint } }
                ?.run {
                    val swaggerWebJarHandler = SwaggerWebJarHandler(
                        swaggerWebJarPath = pluginConfig.webJarPath
                    )
                    router.addEndpoint(
                        SwaggerEndpoint(
                            method = GET,
                            path = "${pluginConfig.webJarPath}/*",
                            roles = pluginConfig.roles.toSet(),
                            handler = swaggerWebJarHandler
                        )
                    )
                }
        }
    }

    override fun repeatable(): Boolean =
        true

}
