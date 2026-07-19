package io.javalin.openapi.experimental

import io.javalin.introspection.AnnotationSet
import io.javalin.introspection.EnumConstant
import io.javalin.introspection.PropertyProjection
import io.javalin.openapi.experimental.processor.generators.TypeSchemaGenerator
import io.javalin.introspection.ClassDefinition as RawType

interface SchemaGenerationContext {

    val typeSchemaGenerator: TypeSchemaGenerator
    val simpleTypeMappings: Map<String, SimpleType>
    val embeddedTypeProcessors: List<EmbeddedTypeProcessor>

    fun isEnum(type: OpenApiType): Boolean

    fun annotationsOf(type: OpenApiType): AnnotationSet

    fun propertiesOf(type: OpenApiType): List<PropertyProjection>

    fun enumConstantsOf(type: OpenApiType): List<EnumConstant>

    fun toOpenApiType(raw: RawType): OpenApiType

    fun acceptsProperty(type: OpenApiType, property: PropertyProjection): Boolean = true

    fun discriminatorSubtypes(type: OpenApiType): List<Pair<String, OpenApiType>> = emptyList()

    fun reportWarning(message: String) {}

    fun reportDebug(message: String) {}
}
