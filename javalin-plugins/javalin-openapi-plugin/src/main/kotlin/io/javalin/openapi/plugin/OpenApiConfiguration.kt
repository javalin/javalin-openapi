@file:Suppress("MemberVisibilityCanBePrivate")

package io.javalin.openapi.plugin

import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.openapi.ApiKeyAuth
import io.javalin.openapi.BasicAuth
import io.javalin.openapi.BearerAuth
import io.javalin.openapi.CookieAuth
import io.javalin.openapi.OAuth2
import io.javalin.openapi.OpenApiInfo
import io.javalin.openapi.OpenApiServer
import io.javalin.openapi.OpenID
import io.javalin.openapi.Security
import io.javalin.openapi.SecurityScheme
import io.javalin.security.RouteRole
import java.util.function.BiConsumer
import java.util.function.Consumer

/** Configure OpenApi plugin */
class OpenApiPluginConfiguration @JvmOverloads constructor(
    @JvmField var documentationPath: String = "/openapi",
    @JvmField var roles: List<RouteRole>? = null,
    @JvmField var prettyOutputEnabled: Boolean = true,
    @JvmField var definitionConfiguration: BiConsumer<String, DefinitionConfiguration>? = null
) {

    /** Path to host documentation as JSON */
    fun withDocumentationPath(path: String): OpenApiPluginConfiguration = also {
        this.documentationPath = path
    }

    /** List of roles eligible to access OpenApi routes */
    fun withRoles(vararg roles: RouteRole): OpenApiPluginConfiguration = also {
        this.roles = roles.toList()
    }

    /** Path to host documentation as JSON */
    @JvmOverloads
    fun withPrettyOutput(enabled: Boolean = true): OpenApiPluginConfiguration = also {
        this.prettyOutputEnabled = enabled
    }

    /* Dynamically apply custom changes to generated OpenApi specifications */
    fun withDefinitionConfiguration(definitionConfigurationConfigurer: BiConsumer<String, DefinitionConfiguration>): OpenApiPluginConfiguration = also {
        definitionConfiguration = definitionConfigurationConfigurer
    }

}

/** Modify OpenApi documentation represented by [ObjectNode] in JSON format */
fun interface DefinitionProcessor {
    fun process(content: ObjectNode): String
}

class DefinitionConfiguration @JvmOverloads constructor(
    @JvmField @JvmSynthetic internal var info: OpenApiInfo? = null,
    @JvmField @JvmSynthetic internal var servers: MutableList<OpenApiServer> = mutableListOf(),
    @JvmField @JvmSynthetic internal var security: SecurityComponentConfiguration? = null,
    @JvmField @JvmSynthetic internal var definitionProcessor: DefinitionProcessor? = null
) {

    /** Define custom info object */
    fun withInfo(openApiInfo: Consumer<OpenApiInfo>): DefinitionConfiguration = also {
        this.info = OpenApiInfo().also { openApiInfo.accept(it) }
    }

    @Deprecated("Use withInfo instead", ReplaceWith("withInfo(openApiInfo)"))
    fun withOpenApiInfo(openApiInfo: Consumer<OpenApiInfo>): DefinitionConfiguration =
        withInfo(openApiInfo)

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

class SecurityComponentConfiguration @JvmOverloads constructor(
    @JvmField @JvmSynthetic internal val securitySchemes: MutableMap<String, SecurityScheme> = mutableMapOf(),
    @JvmField @JvmSynthetic internal val globalSecurity: MutableList<Security> = mutableListOf()
) {

    fun withSecurityScheme(schemeName: String, securityScheme: SecurityScheme): SecurityComponentConfiguration = also {
        securitySchemes[schemeName] = securityScheme
    }

    @JvmOverloads
    fun withBasicAuth(schemeName: String = "BasicAuth", securityScheme: Consumer<BasicAuth> = Consumer {}): SecurityComponentConfiguration =
        withSecurityScheme(schemeName, BasicAuth().also { securityScheme.accept(it) })

    @JvmOverloads
    fun withBearerAuth(schemeName: String = "BearerAuth", securityScheme: Consumer<BearerAuth> = Consumer {}): SecurityComponentConfiguration =
        withSecurityScheme(schemeName, BearerAuth().also { securityScheme.accept(it) })

    @JvmOverloads
    fun withApiKeyAuth(schemeName: String = "ApiKeyAuth", apiKeyHeader: String = "X-Api-Key", securityScheme: Consumer<ApiKeyAuth> = Consumer {}): SecurityComponentConfiguration =
        withSecurityScheme(schemeName, ApiKeyAuth(name = apiKeyHeader).also { securityScheme.accept(it) })

    @JvmOverloads
    fun withCookieAuth(schemeName: String = "CookieAuth", sessionCookie: String = "JSESSIONID", securityScheme: Consumer<CookieAuth> = Consumer {}): SecurityComponentConfiguration =
        withSecurityScheme(schemeName, CookieAuth(name = sessionCookie).also { securityScheme.accept(it) })

    @JvmOverloads
    fun withOpenID(schemeName: String, openIdConnectUrl: String, securityScheme: Consumer<OpenID> = Consumer {}): SecurityComponentConfiguration =
        withSecurityScheme(schemeName, OpenID(openIdConnectUrl = openIdConnectUrl).also { securityScheme.accept(it) })

    @JvmOverloads
    fun withOAuth2(schemeName: String, description: String, securityScheme: Consumer<OAuth2> = Consumer {}): SecurityComponentConfiguration =
        withSecurityScheme(schemeName, OAuth2(description = description).also { securityScheme.accept(it) })

    fun withGlobalSecurity(security: Security): SecurityComponentConfiguration = also {
        globalSecurity.add(security)
    }

    @JvmOverloads
    fun withGlobalSecurity(name: String, security: Consumer<Security> = Consumer {}): SecurityComponentConfiguration =
        withGlobalSecurity(Security(name = name).also { security.accept(it) })

}