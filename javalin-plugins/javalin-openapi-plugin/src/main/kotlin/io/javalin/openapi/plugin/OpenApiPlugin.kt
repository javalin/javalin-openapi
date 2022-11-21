package io.javalin.openapi.plugin

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.Javalin
import io.javalin.json.JavalinJackson
import io.javalin.json.jsonMapper
import io.javalin.openapi.OpenApiInfo
import io.javalin.openapi.OpenApiLoader
import io.javalin.openapi.OpenApiServer
import io.javalin.openapi.Security
import io.javalin.openapi.SecurityScheme
import io.javalin.plugin.Plugin
import io.javalin.security.RouteRole

class OpenApiConfiguration {
    var info: OpenApiInfo = OpenApiInfo()
    var servers: Array<OpenApiServer> = emptyArray()
    var documentationPath = "/openapi"
    var documentProcessor: ((ObjectNode) -> String)? = null
    var security: SecurityConfiguration? = null
    var roles: Array<RouteRole> = emptyArray()
}

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

open class OpenApiPlugin @JvmOverloads constructor(private val configuration: OpenApiConfiguration = OpenApiConfiguration()) : Plugin {

    override fun apply(app: Javalin) {
        app.get(
            configuration.documentationPath,
            OpenApiHandler(createDocumentation(app)),
            *configuration.roles
        )
    }

    private fun createDocumentation(app: Javalin): Lazy<Map<String, String>> =
        lazy {
            with(configuration) {
                val jsonMapper = when (val jsonMapper = app.jsonMapper()) {
                    is JavalinJackson -> jsonMapper.mapper
                    else -> JavalinJackson.defaultMapper()
                }

                OpenApiLoader()
                    .loadVersions()
                    .associateWith { identifier ->
                        val rawDocs = OpenApiPlugin::class.java.getResource("/openapi-plugin/openapi-$identifier.json")
                            ?.readText()
                            ?: "{}"

                        val docsNode = jsonMapper.readTree(rawDocs) as ObjectNode

                        //process OpenAPI "info"
                        docsNode.replace("info", jsonMapper.convertValue(info, JsonNode::class.java))

                        // process OpenAPI "servers"
                        docsNode.replace("servers", jsonMapper.convertValue(servers, JsonNode::class.java))

                        // process OpenAPI "components"
                        val componentsNode = docsNode.get("components") as? ObjectNode?
                            ?: jsonMapper.createObjectNode().also { docsNode.replace("components", it) }

                        // process OpenAPI "securitySchemes"
                        val securitySchemes = security?.securitySchemes ?: emptyMap()
                        componentsNode.replace("securitySchemes", jsonMapper.convertValue(securitySchemes, JsonNode::class.java))

                        //process OpenAPI "security"
                        val securityMap = security?.globalSecurity?.associate { it.name to it.scopes.toTypedArray() }
                        docsNode.replace("security", jsonMapper.convertValue(securityMap, JsonNode::class.java))

                        configuration.documentProcessor
                            ?.invoke(docsNode)
                            ?: docsNode.toPrettyString()
                    }
                }
        }

}