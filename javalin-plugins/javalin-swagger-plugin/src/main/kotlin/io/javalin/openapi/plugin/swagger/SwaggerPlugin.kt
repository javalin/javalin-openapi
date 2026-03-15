package io.javalin.openapi.plugin.swagger

import io.javalin.config.JavalinState
import io.javalin.http.HandlerType.GET
import io.javalin.openapi.OpenApiLoader
import io.javalin.plugin.Plugin
import io.javalin.security.RouteRole
import java.util.function.Consumer

/** Configure Swagger UI plugin */
@Suppress("MemberVisibilityCanBePrivate")
class SwaggerConfiguration @JvmOverloads constructor(
    /** Location of OpenApi documentation */
    @JvmField var documentationPath: String = "/openapi",
    /** Swagger UI route */
    @JvmField var uiPath: String = "/swagger",
    /** Roles eligible to connect to Swagger routes */
    @JvmField var roles: Array<RouteRole> = emptyArray(),
    /** Specify custom base path if Javalin is running behind reverse proxy */
    @JvmField var basePath: String? = null,
    /** Swagger UI Bundle version */
    @JvmField var version: String = "5.31.2",
    /** Swagger UI Bundle webjar location */
    @JvmField var webJarPath: String = "/webjars/swagger-ui",
    /** Page title */
    @JvmField var title: String = "OpenApi documentation",
    /** Specification validator */
    @JvmField var validatorUrl: String? = "https://validator.swagger.io/validator",
    /** Tags sorter algorithm expression. */
    @JvmField var tagsSorter: String = "'alpha'",
    /** Operations sorter algorithm expression. */
    @JvmField var operationsSorter: String = "'alpha'",
    /** Custom version mappings */
    @JvmField var customVersions: MutableList<Pair<String, String>> = arrayListOf(),
    /** Custom CSS files to be injected into Swagger HTML */
    @JvmField var customStylesheetFiles: MutableList<Pair<String, String>> = arrayListOf(),
    /** Custom JavaScript files to be injected into Swagger HTML */
    @JvmField var customJavaScriptFiles: MutableList<Pair<String, String>> = arrayListOf(),
    /** Custom class loader for loading generated OpenAPI resources from classpath */
    @JvmField var resourceClassLoader: ClassLoader? = null,
) {

    /** Set the location of OpenApi documentation */
    fun withDocumentationPath(path: String): SwaggerConfiguration = also { documentationPath = path }

    /** Set the Swagger UI route */
    fun withUiPath(path: String): SwaggerConfiguration = also { uiPath = path }

    /** Set roles eligible to connect to Swagger routes */
    fun withRoles(vararg roles: RouteRole): SwaggerConfiguration = also { this.roles = arrayOf(*roles) }

    /** Set custom base path if Javalin is running behind reverse proxy */
    fun withBasePath(path: String): SwaggerConfiguration = also { basePath = path }

    /** Set Swagger UI Bundle version */
    fun withVersion(version: String): SwaggerConfiguration = also { this.version = version }

    /** Set Swagger UI Bundle webjar location */
    fun withWebJarPath(path: String): SwaggerConfiguration = also { webJarPath = path }

    /** Set page title */
    fun withTitle(title: String): SwaggerConfiguration = also { this.title = title }

    /** Set specification validator URL */
    fun withValidatorUrl(url: String?): SwaggerConfiguration = also { validatorUrl = url }

    /** Set tags sorter algorithm expression */
    fun withTagsSorter(sorter: String): SwaggerConfiguration = also { tagsSorter = sorter }

    /** Set operations sorter algorithm expression */
    fun withOperationsSorter(sorter: String): SwaggerConfiguration = also { operationsSorter = sorter }

    /** Inject custom CSS stylesheet into Swagger UI */
    @JvmOverloads
    fun injectStylesheet(path: String, media: String = "screen"): SwaggerConfiguration = also {
        customStylesheetFiles.add(path to media)
    }

    /** Inject custom JavaScript file into Swagger UI */
    @JvmOverloads
    fun injectJavaScript(path: String, type: String = "text/javascript"): SwaggerConfiguration = also {
        customJavaScriptFiles.add(path to type)
    }

    /** Add custom OpenAPI spec version with external URL */
    fun injectCustomVersion(name: String, url: String): SwaggerConfiguration = also {
        customVersions.add(name to url)
    }

    /** Set custom class loader for loading generated OpenAPI resources from classpath */
    fun withResourceClassLoader(classLoader: ClassLoader): SwaggerConfiguration = also {
        resourceClassLoader = classLoader
    }
}

open class SwaggerPlugin @JvmOverloads constructor(
    userConfig: Consumer<SwaggerConfiguration> = Consumer {},
) : Plugin<SwaggerConfiguration>(userConfig, SwaggerConfiguration()) {

    override fun repeatable(): Boolean = true

    override fun onStart(state: JavalinState) {
        val openApiLoader = OpenApiLoader(pluginConfig.resourceClassLoader ?: OpenApiLoader::class.java.classLoader)
        val versions = openApiLoader.loadVersions()
            .ifEmpty { setOf("default") }
            .map { SwaggerVersionMapping.OpenApiLoader(name = it) }

        val swaggerHandler = SwaggerHandler(
            title = pluginConfig.title,
            documentationPath = pluginConfig.documentationPath,
            versions = versions + pluginConfig.customVersions.map { (name, url) ->
                SwaggerVersionMapping.Custom(name = name, url = url)
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