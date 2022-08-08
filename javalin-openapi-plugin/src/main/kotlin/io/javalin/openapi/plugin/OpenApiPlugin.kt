package io.javalin.openapi.plugin

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.Javalin
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginLifecycleInit
import io.javalin.plugin.json.JavalinJackson.Companion.defaultMapper
import org.slf4j.LoggerFactory

class OpenApiConfiguration {
    var title = "OpenApi Title"
    var description = "OpenApi Description"
    var version = "OpenApi Version"
    var documentationPath = "/openapi"
    var documentProcessor: ((ObjectNode) -> String)? = null
    var security: SecurityConfiguration? = null
}

class OpenApiPlugin(private val configuration: OpenApiConfiguration) : Plugin, PluginLifecycleInit {

    private var documentation: String? = null
    private val logger = LoggerFactory.getLogger(OpenApiPlugin::class.java)

    override fun init(app: Javalin) {
        this.documentation = readResource("/openapi.json")
            ?.replaceFirst("{openapi.title}", configuration.title)
            ?.replaceFirst("{openapi.description}", configuration.description)
            ?.replaceFirst("{openapi.version}", configuration.version)
            ?.let { modifyDocumentation(it) }
    }

    private fun modifyDocumentation(rawDocs: String): String =
        with(configuration) {
            if (documentProcessor == null && security == null) {
                return rawDocs
            }

            val docsNode = defaultMapper().readTree(rawDocs) as ObjectNode
            val componentsNode = docsNode.get("components") as? ObjectNode? ?: defaultMapper().createObjectNode().also { docsNode.replace("components", it) }

            val securitySchemes = security?.securitySchemes ?: emptyMap()
            componentsNode.replace("securitySchemes", defaultMapper().convertValue(securitySchemes, JsonNode::class.java))

            val securityMap = security?.globalSecurity?.associate { it.name to it.scopes.toTypedArray() }
            docsNode.replace("security", defaultMapper().convertValue(securityMap, JsonNode::class.java))

            return configuration.documentProcessor
                ?.invoke(docsNode)
                ?: docsNode.toPrettyString()
        }

    private fun readResource(path: String): String? =
        OpenApiPlugin::class.java.getResource(path)?.readText()

    override fun apply(app: Javalin) {
        if (documentation == null) {
            logger.warn("OpenApi documentation not found")
            return
        }

        app.get(configuration.documentationPath, OpenApiHandler(documentation!!))
    }

}