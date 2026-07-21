package io.javalin.openapi.dynamic.hook

import io.javalin.openapi.OpenApiPluginRouteHandler
import io.javalin.openapi.dynamic.ReflectionSchemaContext
import io.javalin.openapi.plugin.OpenApiHook
import io.javalin.openapi.plugin.OpenApiHookContext
import io.javalin.router.Endpoint
import java.util.function.Consumer

class RegisteredRoutesHookConfiguration {
    private var ignoreDefaultRoutes = true
    private val ignoredPathPrefixes = linkedSetOf<String>()

    fun clearDefaultIgnoredRoutes(): RegisteredRoutesHookConfiguration = apply {
        ignoreDefaultRoutes = false
    }

    fun withIgnoredPathPrefix(prefix: String): RegisteredRoutesHookConfiguration =
        withIgnoredPathPrefixes(prefix)

    fun withIgnoredPathPrefixes(vararg prefixes: String): RegisteredRoutesHookConfiguration = apply {
        prefixes.forEach { prefix ->
            ignoredPathPrefixes.add(normalizePathPrefix(prefix))
        }
    }

    internal fun ignores(endpoint: Endpoint): Boolean =
        (ignoreDefaultRoutes && endpoint.handler is OpenApiPluginRouteHandler) ||
            ignoredPathPrefixes.any { endpoint.path.matchesPathPrefix(it) }

    private fun normalizePathPrefix(prefix: String): String {
        val normalized = prefix.removeSuffix("/*").trimEnd('/').ifEmpty { "/" }
        require(normalized.startsWith('/')) { "Ignored path prefixes must start with '/': $prefix" }
        require('*' !in normalized) { "Ignored path prefixes only support a trailing /*: $prefix" }
        return normalized
    }

    private fun String.matchesPathPrefix(prefix: String): Boolean =
        prefix == "/" || this == prefix || startsWith("$prefix/")
}

class RegisteredRoutesHook @JvmOverloads constructor(
    userConfig: Consumer<RegisteredRoutesHookConfiguration> = Consumer {},
) : OpenApiHook {
    private val config = RegisteredRoutesHookConfiguration().also(userConfig::accept)

    override fun apply(context: OpenApiHookContext) {
        val schemaContext = ReflectionSchemaContext() // document-scoped: owns the generator + its memo cache
        context.builder.openApiVersion("3.1.0")

        for (handler in context.state.internalRouter.allHttpHandlers()) {
            val endpoint = handler.endpoint
            if (!endpoint.method.isHttpMethod || config.ignores(endpoint)) {
                continue
            }

            val path = toOpenApiPath(endpoint.path)
            val method = endpoint.method.name().lowercase()
            val pathParams = PATH_PARAM.findAll(endpoint.path).map { it.groupValues[1] }.toList()
            val metadata = endpoint.metadata(OpenApiMetadata::class.java)
            val operationAlreadyDocumented = context.builder.hasOperation(path, method)

            if (metadata == null && operationAlreadyDocumented) {
                continue
            }

            context.builder.path(path).operation(method) {
                if (!operationAlreadyDocumented && pathParams.isNotEmpty()) {
                    parameters {
                        pathParams.forEach { name ->
                            parameter(name = name, location = "path", required = true) { type("string") }
                        }
                    }
                }

                when (metadata) {
                    null -> responses { response("200") { description("OK") } }
                    else -> metadata.configure(this)
                }
            }
        }

        context.builder.resolveComponentReferences { type -> schemaContext.componentSchema(type) }
    }

    /** Javalin `<slashParam>` -> OpenAPI `{slashParam}`; `{param}` kept as-is. */
    private fun toOpenApiPath(path: String): String =
        path.replace(ANGLE_PARAM) { "{${it.groupValues[1]}}" }

    private companion object {
        private val PATH_PARAM = Regex("[{<]([^}>]+)[}>]")
        private val ANGLE_PARAM = Regex("<([^>]+)>")
    }
}
