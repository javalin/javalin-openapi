package io.javalin.openapi.dynamic.hook

import io.javalin.openapi.dynamic.ReflectiveTypeIntrospector
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

internal val dynamicIntrospector = ReflectiveTypeIntrospector()
internal val dynamicSchemaGenerator = dynamicIntrospector.typeSchemaGenerator

/** Resolve a DTO [type] to a schema `$ref` inside the content DSL, reusing the reflection engine. */
fun MediaTypeBuilder.schema(type: Class<*>): Unit =
    schema(dynamicSchemaGenerator.createEmbeddedTypeDescription(dynamicIntrospector.introspect(type)))
