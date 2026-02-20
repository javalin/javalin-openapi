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
                    val builder = OpenApiSchemaBuilder.fromJson(rawDocs)
                    pluginConfig.definitionConfiguration?.accept(version, builder)
                    when (pluginConfig.definitionProcessor) {
                        null -> if (pluginConfig.prettyOutputEnabled) builder.toJson() else builder.toCompactJson()
                        else -> pluginConfig.definitionProcessor!!.process(ObjectMapper().readTree(builder.toJson()) as ObjectNode)
                    }
                }
        }

}
