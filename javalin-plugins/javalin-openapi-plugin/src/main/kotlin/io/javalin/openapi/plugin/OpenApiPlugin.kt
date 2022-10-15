package io.javalin.openapi.plugin

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.Javalin
import io.javalin.json.JavalinJackson.Companion.defaultMapper
import io.javalin.openapi.OpenApiInfo
import io.javalin.openapi.OpenApiServer
import io.javalin.openapi.Security
import io.javalin.openapi.SecurityScheme
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginLifecycleInit
import io.javalin.security.RouteRole
import org.slf4j.LoggerFactory

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

class OpenApiPlugin @JvmOverloads constructor(private val configuration: OpenApiConfiguration = OpenApiConfiguration()) : Plugin, PluginLifecycleInit {

    private var documentation: String? = null
    private val logger = LoggerFactory.getLogger(OpenApiPlugin::class.java)

    override fun init(app: Javalin) {
        this.documentation = readResource("/openapi-plugin/openapi.json")?.let { modifyDocumentation(it) }
    }

    private fun modifyDocumentation(rawDocs: String): String =
        with(configuration) {
            val docsNode = defaultMapper().readTree(rawDocs) as ObjectNode

            //process OpenAPI "info"
            docsNode.replace("info", defaultMapper().convertValue(info, JsonNode::class.java))

            // process OpenAPI "servers"
            docsNode.replace("servers", defaultMapper().convertValue(servers, JsonNode::class.java))

            // process OpenAPI "components"
            val componentsNode = docsNode.get("components") as? ObjectNode?
                ?: defaultMapper().createObjectNode().also { docsNode.replace("components", it) }

            // process OpenAPI "securitySchemes"
            val securitySchemes = security?.securitySchemes ?: emptyMap()
            componentsNode.replace("securitySchemes", defaultMapper().convertValue(securitySchemes, JsonNode::class.java))

            //process OpenAPI "security"
            val securityMap = security?.globalSecurity?.associate { it.name to it.scopes.toTypedArray() }
            docsNode.replace("security", defaultMapper().convertValue(securityMap, JsonNode::class.java))

            return configuration.documentProcessor
                ?.invoke(docsNode)
                ?: docsNode.toPrettyString()
        }

    override fun apply(app: Javalin) {
        if (documentation == null) {
            logger.warn("OpenApi documentation not found")
            return
        }

        app.get(configuration.documentationPath, OpenApiHandler(documentation!!), *configuration.roles)
    }

    private fun readResource(path: String): String? =
        OpenApiPlugin::class.java.getResource(path)?.readText()

}