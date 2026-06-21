package io.javalin.openapi.experimental

import io.javalin.introspection.Annotations
import io.javalin.introspection.EnumConstantView
import io.javalin.introspection.PropertyView
import io.javalin.introspection.TypeIntrospector
import io.javalin.openapi.OpenApiName
import io.javalin.openapi.experimental.defaults.createDefaultEmbeddedTypeProcessors
import io.javalin.openapi.experimental.defaults.createDefaultSimpleTypeMappings
import io.javalin.openapi.experimental.processor.generators.ResultScheme
import io.javalin.openapi.experimental.processor.generators.TypeSchemaGenerator
import io.javalin.introspection.ClassDefinition as RawType

/**
 * [SchemaGenerationContext] driven entirely by a neutral [TypeIntrospector]: it carries the introspected [RawType]
 * in [OpenApiType.handle] and delegates every navigation call to it. A concrete backend supplies only the
 * [introspector] (runtime reflection, KSP, ...); `@OpenApiName` naming, the default processors, and schema generation
 * are all shared. The annotation processor stays separate because it needs javac-native tokens (round scan, element
 * filtering) that this neutral seam deliberately hides.
 */
abstract class IntrospectorSchemaContext(
    override val simpleTypeMappings: Map<String, SimpleType> = createDefaultSimpleTypeMappings(),
) : SchemaGenerationContext {

    protected abstract val introspector: TypeIntrospector

    override val typeSchemaGenerator: TypeSchemaGenerator = TypeSchemaGenerator(this)
    override val embeddedTypeProcessors: List<EmbeddedTypeProcessor> = createDefaultEmbeddedTypeProcessors()

    /** Resolve a backend-native type token (`Class`, `KSType`, ...) into the OpenAPI model. */
    fun introspect(nativeType: Any): OpenApiType =
        toOpenApiType(introspector.introspect(nativeType))

    /** Full component schema for an already-resolved [type] — the body stored under `components/schemas`. */
    fun componentSchema(type: OpenApiType): ResultScheme =
        typeSchemaGenerator.createTypeSchema(type)

    /** Inline schema (`$ref` or structure) for a backend-native type token. */
    fun inlineSchema(nativeType: Any): ResultScheme =
        typeSchemaGenerator.createEmbeddedTypeDescription(introspect(nativeType))

    override fun toOpenApiType(raw: RawType): OpenApiType {
        val customName = raw.getAnnotations().memberValues(OpenApiName::class.java)?.get("value") as? String
        val packageName = raw.fullName.substringBeforeLast('.', "")
        return OpenApiType(
            simpleName = customName ?: raw.simpleName,
            fullName = when {
                customName == null -> raw.fullName
                packageName.isEmpty() -> customName
                else -> "$packageName.$customName"
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

private val OpenApiType.raw: RawType
    @OptIn(InternalOpenApiTypeApi::class) get() = handle as RawType
