package io.javalin.openapi.plugin

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.config.JavalinState
import io.javalin.openapi.OpenApiLoader
import io.javalin.openapi.schema.OpenApiSchemaBuilder
import io.javalin.plugin.Plugin
import java.util.function.Consumer

open class OpenApiPlugin(userConfig: Consumer<OpenApiPluginConfiguration>) : Plugin<OpenApiPluginConfiguration>(userConfig, OpenApiPluginConfiguration()) {

    override fun repeatable(): Boolean = true

    override fun onStart(state: JavalinState) {
        state.routes.get(
            pluginConfig.documentationPath,
            OpenApiHandler(createDocumentation()),
            *pluginConfig.roles?.toTypedArray() ?: emptyArray()
        )
    }

    private fun createDocumentation(): Lazy<Map<String, String>> =
        lazy {
            OpenApiLoader()
                .loadOpenApiSchemes()
                .mapValues { (version, rawDocs) ->
                    pluginConfig
                        .definitionConfiguration
                        ?.let { DefinitionConfiguration().also { definition -> it.accept(version, definition) } }
                        ?.applyConfigurationTo(
                            content = rawDocs,
                            prettyOutputEnabled = pluginConfig.prettyOutputEnabled
                        )
                        ?: rawDocs
                }
        }

    private fun DefinitionConfiguration.applyConfigurationTo(content: String, prettyOutputEnabled: Boolean): String {
        val builder = OpenApiSchemaBuilder.fromJson(content)

        info?.let { builder.info(it) }

        if (servers.isNotEmpty()) {
            builder.servers(servers)
        }

        security?.let { sec ->
            if (sec.securitySchemes.isNotEmpty()) {
                builder.securitySchemes(sec.securitySchemes)
            }
            if (sec.globalSecurity.isNotEmpty()) {
                builder.globalSecurity(sec.globalSecurity)
            }
        }

        return when (definitionProcessor) {
            null -> if (prettyOutputEnabled) builder.toJson() else builder.toCompactJson()
            else -> definitionProcessor!!.process(ObjectMapper().readTree(builder.toJson()) as ObjectNode)
        }
    }

}
