package io.javalin.openapi.plugin

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
import java.awt.SystemColor
import java.util.function.BiConsumer
import java.util.function.Consumer

const val DEFAULT_DOCS_ID = "*"

/** Configure OpenApi plugin */
class OpenApiConfiguration {

    @JvmSynthetic internal var documentationPath = "/openapi"
    @JvmSynthetic internal var roles: Array<out RouteRole> = emptyArray()
    @JvmSynthetic internal var definitionConfiguration: BiConsumer<String, DefinitionConfiguration>? = null

    /** Path to host documentation as JSON */
    fun withDocumentationPath(path: String): OpenApiConfiguration = also {
        this.documentationPath = path
    }

    /** List of roles eligible to access OpenApi routes */
    fun withRoles(vararg roles: RouteRole): OpenApiConfiguration = also {
        this.roles = roles
    }

    /* */
    fun withDefinitionConfiguration(definitionConfigurationConfigurer: BiConsumer<String, DefinitionConfiguration>): OpenApiConfiguration = also {
        definitionConfiguration = definitionConfigurationConfigurer
    }

}

/** Modify OpenApi documentation represented by [ObjectNode] in JSON format */
fun interface DefinitionProcessor {
    fun process(content: ObjectNode): String
}

class DefinitionConfiguration {

    internal var info: OpenApiInfo? = null
    internal var servers: MutableList<OpenApiServer> = mutableListOf()
    internal var security: SecurityConfiguration? = null
    internal var definitionProcessor: DefinitionProcessor? = null

    /** Define custom info object */
    fun withOpenApiInfo(openApiInfo: Consumer<OpenApiInfo>): DefinitionConfiguration = also {
        this.info = OpenApiInfo().also { openApiInfo.accept(it) }
    }

    /** Add custom server **/
    fun withServer(serverConfigurer: Consumer<OpenApiServer>): DefinitionConfiguration = also {
        this.servers.add(OpenApiServer().also { serverConfigurer.accept(it) })
    }

    /** Define custom security object */
    fun withSecurity(securityConfiguration: SecurityConfiguration): DefinitionConfiguration = also {
        this.security = securityConfiguration
    }

    /** Register scheme processor */
    fun withDefinitionProcessor(definitionProcessor: DefinitionProcessor): DefinitionConfiguration = also {
        this.definitionProcessor = definitionProcessor
    }

}

data class SecurityConfiguration @JvmOverloads constructor(
    internal val securitySchemes: MutableMap<String, SecurityScheme> = mutableMapOf(),
    internal val globalSecurity: MutableList<Security> = mutableListOf()
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
            val jsonMapper = when (val jsonMapper = app.jsonMapper()) {
                is JavalinJackson -> jsonMapper.mapper
                else -> JavalinJackson.defaultMapper()
            }

            OpenApiLoader()
                .loadOpenApiSchemes()
                .mapValues { (version, rawDocs) ->
                    configuration.definitionConfiguration
                        ?.let { DefinitionConfiguration().also { definition -> it.accept(version, definition) } }
                        ?.applyConfigurationTo(jsonMapper, version, rawDocs)
                        ?: rawDocs
                }
        }

    private fun DefinitionConfiguration.applyConfigurationTo(jsonMapper: ObjectMapper, version: String, content: String): String {
        val docsNode = jsonMapper.readTree(content) as ObjectNode

        //process OpenAPI "info"
        docsNode.replace("info", jsonMapper.convertValue(SystemColor.info, JsonNode::class.java))

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

        return definitionProcessor
            ?.process(docsNode)
            ?: docsNode.toPrettyString()
    }

}