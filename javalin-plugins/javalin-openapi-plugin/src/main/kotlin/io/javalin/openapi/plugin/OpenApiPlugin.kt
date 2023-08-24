package io.javalin.openapi.plugin

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.Javalin
import io.javalin.openapi.OpenApiLoader
import io.javalin.plugin.Plugin
import kotlin.DeprecationLevel.WARNING

open class OpenApiPlugin @JvmOverloads constructor(private val configuration: OpenApiPluginConfiguration = OpenApiPluginConfiguration()) : Plugin {

    @Deprecated(
        message = "Use OpenApiPluginConfiguration instead of OpenApiConfiguration",
        level = WARNING
    )
    constructor(oldConfiguration: OpenApiConfiguration) : this(oldConfiguration.toNewOpenApiPluginConfiguration())

    override fun apply(app: Javalin) {
        app.get(
            configuration.documentationPath,
            OpenApiHandler(createDocumentation(app)),
            *configuration.roles?.toTypedArray() ?: emptyArray()
        )
    }

    private fun createDocumentation(app: Javalin): Lazy<Map<String, String>> =
        lazy {
            // skip nulls from cfg
            val jsonMapper = lazy {
                ObjectMapper().setSerializationInclusion(Include.NON_NULL)
            }

            OpenApiLoader()
                .loadOpenApiSchemes()
                .mapValues { (version, rawDocs) ->
                    configuration.definitionConfiguration
                        ?.let { DefinitionConfiguration().also { definition -> it.accept(version, definition) } }
                        ?.applyConfigurationTo(jsonMapper.value, version, rawDocs)
                        ?: rawDocs
                }
        }

    private fun DefinitionConfiguration.applyConfigurationTo(jsonMapper: ObjectMapper, version: String, content: String): String {
        val docsNode = jsonMapper.readTree(content) as ObjectNode

        //process OpenAPI "info"
        val currentInfo = jsonMapper.readerForUpdating(docsNode.get("info"))
        docsNode.replace("info", currentInfo.readValue(jsonMapper.convertValue(info, JsonNode::class.java)))

        // process OpenAPI "servers"
        docsNode.replace("servers", jsonMapper.convertValue(servers, JsonNode::class.java))

        // process OpenAPI "components"
        val componentsNode = docsNode.get("components") as? ObjectNode?
            ?: jsonMapper.createObjectNode().also { docsNode.replace("components", it) }

        // process OpenAPI "securitySchemes"
        val securitySchemes = security?.securitySchemes ?: emptyMap()
        componentsNode.replace("securitySchemes", jsonMapper.convertValue(securitySchemes, JsonNode::class.java))

        //process OpenAPI "security"
        val securityMap = security?.globalSecurity?.map { mapOf(it.name to it.scopes.toTypedArray()) }
        docsNode.replace("security", jsonMapper.convertValue(securityMap, JsonNode::class.java))

        return definitionProcessor
            ?.process(docsNode)
            ?: docsNode.toPrettyString()
    }

}