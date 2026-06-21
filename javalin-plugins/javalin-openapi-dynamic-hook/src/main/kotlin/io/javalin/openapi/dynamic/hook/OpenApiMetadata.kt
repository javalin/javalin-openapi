package io.javalin.openapi.dynamic.hook

import io.javalin.openapi.dynamic.ReflectionSchemaContext
import io.javalin.openapi.schema.MediaTypeBuilder
import io.javalin.openapi.schema.OperationBuilder
import io.javalin.router.EndpointMetadata
import java.util.function.Consumer

class OpenApiMetadata(val configure: OperationBuilder.() -> Unit) : EndpointMetadata {

    companion object {
        @JvmStatic
        fun of(configure: Consumer<OperationBuilder>): OpenApiMetadata =
            OpenApiMetadata { configure.accept(this) }
    }
}

fun MediaTypeBuilder.schema(type: Class<*>): Unit =
    schema(ReflectionSchemaContext().inlineSchema(type))
