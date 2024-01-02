package io.javalin.openapi.plugin

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.config.JavalinConfig
import io.javalin.openapi.OpenApiLoader
import io.javalin.plugin.Plugin
import java.util.function.Consumer

open class OpenApiPlugin(userConfig: Consumer<OpenApiPluginConfiguration>) : Plugin<OpenApiPluginConfiguration>(userConfig, OpenApiPluginConfiguration()) {

    override fun onStart(config: JavalinConfig) {
        config.router.mount {
            it.get(
                pluginConfig.documentationPath,
                OpenApiHandler(createDocumentation()),
                *pluginConfig.roles?.toTypedArray() ?: emptyArray()
            )
        }
    }

    private fun createDocumentation(): Lazy<Map<String, String>> =
        lazy {
            // skip nulls from cfg
            val jsonMapper = lazy {
                ObjectMapper().setSerializationInclusion(Include.NON_NULL)
            }

            OpenApiLoader()
                .loadOpenApiSchemes()
                .mapValues { (version, rawDocs) ->
                    pluginConfig
                        .definitionConfiguration
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