package io.javalin.openapi.dynamic.hook

import io.javalin.openapi.dynamic.ReflectionSchemaContext
import io.javalin.openapi.schema.MediaTypeBuilder
import io.javalin.openapi.schema.OperationBuilder
import io.javalin.router.EndpointMetadata
import java.util.function.Consumer

/** Endpoint metadata documenting a route through the OpenApi [OperationBuilder] DSL. */
class OpenApiMetadata(val configure: OperationBuilder.() -> Unit) : EndpointMetadata {

    companion object {
        /** Java-friendly factory. */
        @JvmStatic
        fun of(configure: Consumer<OperationBuilder>): OpenApiMetadata =
            OpenApiMetadata { configure.accept(this) }
    }
}

/** Resolve a DTO [type] to a schema inside the content DSL via the reflection engine. */
fun MediaTypeBuilder.schema(type: Class<*>): Unit =
    schema(ReflectionSchemaContext().inlineSchema(type))
