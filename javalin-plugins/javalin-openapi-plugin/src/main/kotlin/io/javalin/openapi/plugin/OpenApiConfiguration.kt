@file:Suppress("MemberVisibilityCanBePrivate")

package io.javalin.openapi.plugin

import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.config.JavalinState
import io.javalin.openapi.schema.OpenApiSchemaBuilder
import io.javalin.security.RouteRole
import java.util.function.BiConsumer

/** Modify OpenApi documentation represented by ObjectNode */
fun interface DefinitionProcessor {
    fun process(content: ObjectNode): String
}

/** Context for an [OpenApiHook]: the document [builder] for a [version], plus the live Javalin [state]. */
class OpenApiHookContext(
    val version: String,
    val builder: OpenApiSchemaBuilder,
    val state: JavalinState,
)

/** Hook that extends the OpenApi document at runtime, per version. */
fun interface OpenApiHook {
    fun apply(context: OpenApiHookContext)
}

/** Configure OpenApi plugin */
class OpenApiPluginConfiguration @JvmOverloads constructor(
    @JvmField var documentationPath: String = "/openapi",
    @JvmField var roles: Array<RouteRole> = emptyArray(),
    @JvmField var prettyOutputEnabled: Boolean = true,
    @JvmField var definitionConfiguration: BiConsumer<String, OpenApiSchemaBuilder>? = null,
    @JvmField var definitionProcessor: DefinitionProcessor? = null,
    @JvmField var resourceClassLoader: ClassLoader? = null,
    @JvmField var hooks: MutableList<OpenApiHook> = mutableListOf(),
) {

    /** Register a hook that extends the document at runtime (e.g. dynamically generated routes) */
    fun withHook(hook: OpenApiHook): OpenApiPluginConfiguration = apply {
        this.hooks.add(hook)
    }

    /** Path to host documentation as JSON */
    fun withDocumentationPath(path: String): OpenApiPluginConfiguration = apply {
        this.documentationPath = path
    }

    /** List of roles eligible to access OpenApi routes */
    fun withRoles(vararg roles: RouteRole): OpenApiPluginConfiguration = apply {
        this.roles = arrayOf(*roles)
    }

    /** Pretty print JSON output */
    @JvmOverloads
    fun withPrettyOutput(enabled: Boolean = true): OpenApiPluginConfiguration = apply {
        this.prettyOutputEnabled = enabled
    }

    /** Dynamically apply custom changes to generated OpenApi specifications */
    fun withDefinitionConfiguration(
        definitionConfigurationConfigurer: BiConsumer<String, OpenApiSchemaBuilder>,
    ): OpenApiPluginConfiguration = apply {
        this.definitionConfiguration = definitionConfigurationConfigurer
    }

    /** Global definition processor applied to all versions without their own processor */
    fun withDefinitionProcessor(definitionProcessor: DefinitionProcessor): OpenApiPluginConfiguration = apply {
        this.definitionProcessor = definitionProcessor
    }

    /** Set custom class loader for loading generated OpenAPI resources from classpath */
    fun withResourceClassLoader(classLoader: ClassLoader): OpenApiPluginConfiguration = apply {
        this.resourceClassLoader = classLoader
    }
}
