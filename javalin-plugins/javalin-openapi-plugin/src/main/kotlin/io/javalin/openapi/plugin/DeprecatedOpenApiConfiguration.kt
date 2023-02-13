package io.javalin.openapi.plugin

import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.openapi.OpenApiInfo
import io.javalin.openapi.OpenApiServer
import io.javalin.openapi.Security
import io.javalin.openapi.SecurityScheme
import io.javalin.security.RouteRole
import kotlin.DeprecationLevel.WARNING

@Deprecated(
    message = "Use OpenApiPluginConfiguration",
    level = WARNING
)
class OpenApiConfiguration {
    var info: OpenApiInfo = OpenApiInfo()
    var servers: Array<OpenApiServer> = emptyArray()
    var documentationPath = "/openapi"
    var documentProcessor: ((ObjectNode) -> String)? = null
    var security: SecurityConfiguration? = null
    var roles: Array<RouteRole> = emptyArray()
}

@Deprecated(
    message = "Use OpenApiPluginConfiguration with SecurityComponentConfiguration",
    level = WARNING
)
data class SecurityConfiguration @JvmOverloads constructor(
    val securitySchemes: MutableMap<String, SecurityScheme> = mutableMapOf(),
    val globalSecurity: MutableList<Security> = mutableListOf()
) {

    fun withSecurityScheme(schemeName: String, securityScheme: SecurityScheme): SecurityConfiguration = also {
        securitySchemes[schemeName] = securityScheme
    }

    fun withSecurity(security: Security): SecurityConfiguration = also {
        globalSecurity.add(security)
    }

}

internal fun OpenApiConfiguration.toNewOpenApiPluginConfiguration(): OpenApiPluginConfiguration =
    OpenApiPluginConfiguration()
        .withDocumentationPath(documentationPath)
        .withRoles(*roles)
        .withDefinitionConfiguration { _, definition ->
            definition.withOpenApiInfo {
                it.title = info.title
                it.summary = info.summary
                it.description = info.description
                it.termsOfService = info.termsOfService
                it.contact = info.contact
                it.license = info.license
                it.version = info.version
            }

            security?.also { oldSecurity ->
                definition.withSecurity { security ->
                    security.globalSecurity.addAll(oldSecurity.globalSecurity)
                    security.securitySchemes.putAll(oldSecurity.securitySchemes)
                }
            }

            servers.onEach { oldServer ->
                definition.withServer(oldServer)
            }

            documentProcessor?.also { oldProcessor ->
                definition.withDefinitionProcessor { content ->
                    oldProcessor.invoke(content)
                }
            }
        }