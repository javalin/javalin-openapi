package io.javalin.openapi.dynamic

import io.javalin.introspection.Annotations
import io.javalin.introspection.EnumConstantView
import io.javalin.introspection.InternalIntrospectionApi
import io.javalin.introspection.PropertyView
import io.javalin.introspection.runtime.ReflectionTypeIntrospector
import io.javalin.openapi.OpenApiName
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.EmbeddedTypeProcessor
import io.javalin.openapi.experimental.SchemaGenerationContext
import io.javalin.openapi.experimental.SimpleType
import io.javalin.openapi.experimental.StructureType
import io.javalin.openapi.experimental.defaults.ArrayEmbeddedTypeProcessor
import io.javalin.openapi.experimental.defaults.CompositionEmbeddedTypeProcessor
import io.javalin.openapi.experimental.defaults.DictionaryEmbeddedTypeProcessor
import io.javalin.openapi.experimental.defaults.createDefaultSimpleTypeMappings
import io.javalin.openapi.experimental.processor.generators.TypeSchemaGenerator
import java.lang.reflect.Type
import io.javalin.introspection.ClassDefinition as RawType

/** Runtime-reflection [SchemaGenerationContext]: drives the shared [TypeSchemaGenerator] over [ReflectionTypeIntrospector]. */
class ReflectiveTypeIntrospector(
    override val simpleTypeMappings: Map<String, SimpleType> = createDefaultSimpleTypeMappings(),
) : SchemaGenerationContext {

    private val runtime = ReflectionTypeIntrospector()

    override val typeSchemaGenerator: TypeSchemaGenerator = TypeSchemaGenerator(this)
    override val embeddedTypeProcessors: List<EmbeddedTypeProcessor> = listOf(
        CompositionEmbeddedTypeProcessor(),
        ArrayEmbeddedTypeProcessor(),
        DictionaryEmbeddedTypeProcessor()
    )

    fun introspect(type: Type): ClassDefinition =
        toClassDefinition(runtime.introspect(type))

    /** Map the policy-free [RawType] tree into the OpenAPI [ClassDefinition] model, applying `@OpenApiName`. */
    @OptIn(InternalIntrospectionApi::class)
    override fun toClassDefinition(raw: RawType): ClassDefinition {
        val erasure = raw.source as Class<*>
        val customName = erasure.getAnnotation(OpenApiName::class.java)?.value
        val packageName = erasure.`package`?.name?.takeIf { it.isNotEmpty() }
        return ClassDefinition(
            simpleName = customName ?: raw.simpleName,
            fullName = when {
                customName != null -> if (packageName == null) customName else "$packageName.$customName"
                else -> raw.fullName
            },
            generics = raw.generics.map { toClassDefinition(it) },
            structureType = StructureType.valueOf(raw.structureType.name),
            handle = raw,
        )
    }

    override fun isEnum(type: ClassDefinition): Boolean =
        type.raw.isEnum()

    override fun annotationsOf(type: ClassDefinition): Annotations =
        type.raw.getAnnotations()

    override fun propertiesOf(type: ClassDefinition): List<PropertyView> =
        type.raw.getProperties()

    override fun enumConstantsOf(type: ClassDefinition): List<EnumConstantView> =
        type.raw.getEnumConstants() ?: emptyList()

}

private val ClassDefinition.raw: RawType
    get() = handle as RawType
