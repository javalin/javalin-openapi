package io.javalin.openapi.experimental

import io.javalin.introspection.Annotations
import io.javalin.introspection.EnumConstantView
import io.javalin.introspection.PropertyView
import io.javalin.openapi.experimental.processor.generators.TypeSchemaGenerator
import io.javalin.introspection.ClassDefinition as RawType

/**
 * Neutral platform seam the shared schema generator runs against. Each backend (annotation processing, runtime
 * reflection, ...) supplies introspection, class conversion, property filtering, and discriminator subtypes; the
 * generator itself stays free of any single type system.
 */
interface SchemaGenerationContext {

    val typeSchemaGenerator: TypeSchemaGenerator
    val simpleTypeMappings: Map<String, SimpleType>
    val embeddedTypeProcessors: List<EmbeddedTypeProcessor>

    fun isEnum(type: OpenApiType): Boolean

    fun annotationsOf(type: OpenApiType): Annotations

    fun propertiesOf(type: OpenApiType): List<PropertyView>

    fun enumConstantsOf(type: OpenApiType): List<EnumConstantView>

    /** Lift a backend-neutral [RawType] into the OpenAPI [OpenApiType] model (names + handle stay backend-specific). */
    fun toOpenApiType(raw: RawType): OpenApiType

    /** Whether [property] of [type] should be emitted (platform-specific filtering, e.g. the AP `propertyInSchemeFilter`). */
    fun acceptsProperty(type: OpenApiType, property: PropertyView): Boolean = true

    /** Discriminator subtypes discovered by the platform (AP round scan); empty when the backend cannot scan. */
    fun discriminatorSubtypes(type: OpenApiType): List<Pair<String, OpenApiType>> = emptyList()
}
