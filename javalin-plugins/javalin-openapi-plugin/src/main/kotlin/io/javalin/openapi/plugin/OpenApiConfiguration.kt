package io.javalin.openapi.plugin

import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.openapi.OpenApiInfo
import io.javalin.openapi.OpenApiServer
import io.javalin.openapi.Security
import io.javalin.openapi.SecurityScheme
import io.javalin.security.RouteRole
import java.util.function.BiConsumer
import java.util.function.Consumer

/** Configure OpenApi plugin */
data class OpenApiPluginConfiguration @JvmOverloads constructor(
    @JvmField @JvmSynthetic internal var documentationPath: String = "/openapi",
    @JvmField @JvmSynthetic internal var roles: List<RouteRole>? = null,
    @JvmField @JvmSynthetic internal var definitionConfiguration: BiConsumer<String, DefinitionConfiguration>? = null
) {

    /** Path to host documentation as JSON */
    fun withDocumentationPath(path: String): OpenApiPluginConfiguration = also {
        this.documentationPath = path
    }

    /** List of roles eligible to access OpenApi routes */
    fun withRoles(vararg roles: RouteRole): OpenApiPluginConfiguration = also {
        this.roles = roles.toList()
    }

    /* */
    fun withDefinitionConfiguration(definitionConfigurationConfigurer: BiConsumer<String, DefinitionConfiguration>): OpenApiPluginConfiguration = also {
        definitionConfiguration = definitionConfigurationConfigurer
    }

}

/** Modify OpenApi documentation represented by [ObjectNode] in JSON format */
fun interface DefinitionProcessor {
    fun process(content: ObjectNode): String
}

data class DefinitionConfiguration @JvmOverloads constructor(
    @JvmField @JvmSynthetic internal var info: OpenApiInfo? = null,
    @JvmField @JvmSynthetic internal var servers: MutableList<OpenApiServer> = mutableListOf(),
    @JvmField @JvmSynthetic internal var security: SecurityComponentConfiguration? = null,
    @JvmField @JvmSynthetic internal var definitionProcessor: DefinitionProcessor? = null
) {

    /** Define custom info object */
    fun withOpenApiInfo(openApiInfo: Consumer<OpenApiInfo>): DefinitionConfiguration = also {
        this.info = OpenApiInfo().also { openApiInfo.accept(it) }
    }

    /** Add custom server **/
    fun withServer(server: OpenApiServer): DefinitionConfiguration = also {
        this.servers.add(server)
    }

    /** Add custom server **/
    fun withServer(serverConfigurer: Consumer<OpenApiServer>): DefinitionConfiguration = also {
        this.servers.add(OpenApiServer().also { serverConfigurer.accept(it) })
    }

    /** Define custom security object */
    fun withSecurity(securityConfigurer: Consumer<SecurityComponentConfiguration>): DefinitionConfiguration = also {
        SecurityComponentConfiguration()
            .also { securityConfigurer.accept(it) }
            .let { withSecurity(it) }
    }

    /** Define custom security object */
    fun withSecurity(securityConfiguration: SecurityComponentConfiguration): DefinitionConfiguration = also {
        this.security = securityConfiguration
    }

    /** Register scheme processor */
    fun withDefinitionProcessor(definitionProcessor: DefinitionProcessor): DefinitionConfiguration = also {
        this.definitionProcessor = definitionProcessor
    }

}

data class SecurityComponentConfiguration @JvmOverloads constructor(
    @JvmField @JvmSynthetic internal val securitySchemes: MutableMap<String, SecurityScheme> = mutableMapOf(),
    @JvmField @JvmSynthetic internal val globalSecurity: MutableList<Security> = mutableListOf()
) {

    fun withSecurityScheme(schemeName: String, securityScheme: SecurityScheme): SecurityComponentConfiguration = also {
        securitySchemes[schemeName] = securityScheme
    }

    fun withGlobalSecurity(security: Security): SecurityComponentConfiguration = also {
        globalSecurity.add(security)
    }

}