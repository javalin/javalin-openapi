package io.javalin.openapi.dynamic.hook

import io.javalin.openapi.dynamic.ReflectionSchemaContext
import io.javalin.openapi.plugin.OpenApiHook
import io.javalin.openapi.plugin.OpenApiHookContext

class RegisteredRoutesHook : OpenApiHook {

    override fun apply(context: OpenApiHookContext) {
        val schemaContext = ReflectionSchemaContext() // document-scoped: owns the generator + its memo cache
        context.builder.openApiVersion("3.1.0")

        context.state.internalRouter.allHttpHandlers()
            .map { it.endpoint }
            .filter { it.method.isHttpMethod }
            .forEach { endpoint ->
                val pathParams = PATH_PARAM.findAll(endpoint.path).map { it.groupValues[1] }.toList()
                val metadata = endpoint.metadata(OpenApiMetadata::class.java)

                context.builder.path(toOpenApiPath(endpoint.path)).operation(endpoint.method.name().lowercase()) {
                    if (pathParams.isNotEmpty()) {
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
