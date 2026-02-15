package io.javalin.openapi.plugin.swagger

import io.javalin.config.JavalinState
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
    var version = "5.17.14"
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
    /** Custom version mappings */
    var customVersions: MutableList<Pair<String, String>> = arrayListOf()
    /**  Custom CSS files to be injected into Swagger HTML */
    var customStylesheetFiles: MutableList<Pair<String, String>> = arrayListOf()
    /**  Custom JavaScript files to be injected into Swagger HTML */
    var customJavaScriptFiles: MutableList<Pair<String, String>> = arrayListOf()

    @JvmOverloads
    fun injectStylesheet(path: String, media: String = "screen"): SwaggerConfiguration = also {
        customStylesheetFiles.add(path to media)
    }

    @JvmOverloads
    fun injectJavaScript(path: String, type: String = "text/javascript"): SwaggerConfiguration = also  {
        customJavaScriptFiles.add(path to type)
    }

    fun injectCustomVersion(name: String, url: String): SwaggerConfiguration = also {
        customVersions.add(name to url)
    }
}

open class SwaggerPlugin @JvmOverloads constructor(
    userConfig: Consumer<SwaggerConfiguration> = Consumer {},
) : Plugin<SwaggerConfiguration>(userConfig, SwaggerConfiguration()) {

    override fun repeatable(): Boolean = true

    override fun onStart(state: JavalinState) {
        val openApiLoader = OpenApiLoader()
        val versions = openApiLoader.loadVersions().map {
            SwaggerVersionMapping(
                name = it,
                type = SwaggerVersionMapping.SwaggerVersionType.OPENAPI_LOADER
            )
        }

        val swaggerHandler = SwaggerHandler(
            title = pluginConfig.title,
            documentationPath = pluginConfig.documentationPath,
            versions = versions + pluginConfig.customVersions.map { (name, url) ->
                SwaggerVersionMapping(
                    name = name,
                    url = url,
                    type = SwaggerVersionMapping.SwaggerVersionType.CUSTOM
                )
            },
            swaggerVersion = pluginConfig.version,
            validatorUrl = pluginConfig.validatorUrl,
            routingPath = state.router.contextPath,
            basePath = pluginConfig.basePath,
            tagsSorter = pluginConfig.tagsSorter,
            operationsSorter = pluginConfig.operationsSorter,
            customStylesheetFiles = pluginConfig.customStylesheetFiles,
            customJavaScriptFiles = pluginConfig.customJavaScriptFiles
        )

        val swaggerEndpoint = SwaggerEndpoint(
            method = GET,
            path = pluginConfig.uiPath,
            roles = pluginConfig.roles.toSet(),
            handler = swaggerHandler
        )

        state.routes.let { router ->
            /** Register handler for swagger ui */
            router.addEndpoint(swaggerEndpoint)

            /** Register webjar handler if and only if there isn't already a [SwaggerWebJarHandler] at configured route */
            state.internalRouter
                .findHttpHandlerEntries(GET, "${pluginConfig.webJarPath}/*")
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

}