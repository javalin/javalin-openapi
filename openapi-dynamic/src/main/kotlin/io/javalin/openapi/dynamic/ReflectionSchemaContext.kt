package io.javalin.openapi.dynamic

import io.javalin.introspection.Annotations
import io.javalin.introspection.EnumConstantView
import io.javalin.introspection.InternalIntrospectionApi
import io.javalin.introspection.PropertyView
import io.javalin.introspection.runtime.ReflectionTypeIntrospector
import io.javalin.openapi.OpenApiName
import io.javalin.openapi.experimental.OpenApiType
import io.javalin.openapi.experimental.InternalOpenApiTypeApi
import io.javalin.openapi.experimental.EmbeddedTypeProcessor
import io.javalin.openapi.experimental.SchemaGenerationContext
import io.javalin.openapi.experimental.SimpleType
import io.javalin.openapi.experimental.StructureType
import io.javalin.openapi.experimental.defaults.createDefaultEmbeddedTypeProcessors
import io.javalin.openapi.experimental.defaults.createDefaultSimpleTypeMappings
import io.javalin.openapi.experimental.processor.generators.ResultScheme
import io.javalin.openapi.experimental.processor.generators.TypeSchemaGenerator
import java.lang.reflect.Type
import io.javalin.introspection.ClassDefinition as RawType

/**
 * Runtime-reflection [SchemaGenerationContext] — the reflection counterpart of the annotation processor's context.
 * Resolves [Type]s into the OpenAPI model and drives the shared [TypeSchemaGenerator]. Stateless beyond the generator's
 * memo cache, so a fresh instance per document build keeps that cache document-scoped.
 */
class ReflectionSchemaContext(
    override val simpleTypeMappings: Map<String, SimpleType> = createDefaultSimpleTypeMappings(),
) : SchemaGenerationContext {

    private val runtime = ReflectionTypeIntrospector()

    override val typeSchemaGenerator: TypeSchemaGenerator = TypeSchemaGenerator(this)
    override val embeddedTypeProcessors: List<EmbeddedTypeProcessor> = createDefaultEmbeddedTypeProcessors()

    /** Resolve a reflection [type] into the OpenAPI [OpenApiType] model, applying `@OpenApiName`. */
    fun introspect(type: Type): OpenApiType =
        toOpenApiType(runtime.introspect(type))

    /** Full component schema for an already-resolved [type] — the body stored under `components/schemas`. */
    fun componentSchema(type: OpenApiType): ResultScheme =
        typeSchemaGenerator.createTypeSchema(type)

    /** Inline schema (`$ref` or structure) for a raw reflection [type]. */
    fun inlineSchema(type: Type): ResultScheme =
        typeSchemaGenerator.createEmbeddedTypeDescription(introspect(type))

    @OptIn(InternalIntrospectionApi::class)
    override fun toOpenApiType(raw: RawType): OpenApiType {
        val erasure = raw.source as Class<*>
        val customName = erasure.getAnnotation(OpenApiName::class.java)?.value
        val packageName = erasure.`package`?.name?.takeIf { it.isNotEmpty() }
        return OpenApiType(
            simpleName = customName ?: raw.simpleName,
            fullName = when {
                customName != null -> if (packageName == null) customName else "$packageName.$customName"
                else -> raw.fullName
            },
            generics = raw.generics.map { toOpenApiType(it) },
            structureType = StructureType.valueOf(raw.structureType.name),
            handle = raw,
        )
    }

    override fun isEnum(type: OpenApiType): Boolean =
        type.raw.isEnum()

    override fun annotationsOf(type: OpenApiType): Annotations =
        type.raw.getAnnotations()

    override fun propertiesOf(type: OpenApiType): List<PropertyView> =
        type.raw.getProperties()

    override fun enumConstantsOf(type: OpenApiType): List<EnumConstantView> =
        type.raw.getEnumConstants() ?: emptyList()

}

@OptIn(InternalOpenApiTypeApi::class)
private val OpenApiType.raw: RawType
    get() = handle as RawType
