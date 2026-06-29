package io.javalin.openapi.experimental

import io.javalin.introspection.AnnotationSet
import io.javalin.introspection.EnumConstantView
import io.javalin.introspection.PropertyView
import io.javalin.openapi.experimental.processor.generators.TypeSchemaGenerator
import io.javalin.introspection.ClassDefinition as RawType

interface SchemaGenerationContext {

    val typeSchemaGenerator: TypeSchemaGenerator
    val simpleTypeMappings: Map<String, SimpleType>
    val embeddedTypeProcessors: List<EmbeddedTypeProcessor>

    fun isEnum(type: OpenApiType): Boolean

    fun annotationsOf(type: OpenApiType): AnnotationSet

    fun propertiesOf(type: OpenApiType): List<PropertyView>

    fun enumConstantsOf(type: OpenApiType): List<EnumConstantView>

    fun toOpenApiType(raw: RawType): OpenApiType

    fun acceptsProperty(type: OpenApiType, property: PropertyView): Boolean = true

    fun discriminatorSubtypes(type: OpenApiType): List<Pair<String, OpenApiType>> = emptyList()
}
