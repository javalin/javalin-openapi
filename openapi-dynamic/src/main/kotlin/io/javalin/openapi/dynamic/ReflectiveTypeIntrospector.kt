package io.javalin.openapi.dynamic

import io.javalin.introspection.Accessor
import io.javalin.introspection.runtime.ReflectionTypeIntrospector
import io.javalin.openapi.OpenApiByFields
import io.javalin.openapi.OpenApiDescription
import io.javalin.openapi.OpenApiIgnore
import io.javalin.openapi.OpenApiName
import io.javalin.openapi.OpenApiNaming
import io.javalin.openapi.OpenApiNullable
import io.javalin.openapi.OpenApiRequired
import io.javalin.openapi.Visibility
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.StructureType
import io.javalin.openapi.experimental.TypeIntrospector
import io.javalin.openapi.experimental.processor.generators.Property
import io.javalin.openapi.experimental.processor.generators.translatePropertyName
import java.lang.reflect.Type
import io.javalin.introspection.ClassDefinition as RawType
import io.javalin.introspection.PropertyView as RawProperty
import io.javalin.introspection.Visibility as RawVisibility

/** Runtime-reflection [TypeIntrospector]: layers OpenAPI policy over the shared [ReflectionTypeIntrospector]. */
class ReflectiveTypeIntrospector(
    private val requireNonNullsByDefault: Boolean = true,
) : TypeIntrospector {

    private val runtime = ReflectionTypeIntrospector()

    override fun introspect(nativeType: Any): ClassDefinition {
        require(nativeType is Type) {
            "ReflectiveTypeIntrospector expects a java.lang.reflect.Type, got ${nativeType::class.java.name}"
        }
        return introspect(nativeType)
    }

    fun introspect(type: Type): ClassDefinition =
        toExperimental(runtime.introspect(type))

    /** Map the policy-free [RawType] tree into the OpenAPI [ClassDefinition] model, applying `@OpenApiName`. */
    private fun toExperimental(raw: RawType): ClassDefinition {
        val erasure = raw.source as Class<*>
        val customName = erasure.getAnnotation(OpenApiName::class.java)?.value
        val packageName = erasure.`package`?.name?.takeIf { it.isNotEmpty() }
        return ClassDefinition(
            simpleName = customName ?: raw.simpleName,
            fullName = when {
                customName != null -> if (packageName == null) customName else "$packageName.$customName"
                else -> raw.fullName
            },
            generics = raw.generics.map { toExperimental(it) },
            structureType = StructureType.valueOf(raw.structureType.name),
            handle = raw,
        )
    }

    override fun properties(type: ClassDefinition): List<Property> {
        val raw = type.raw
        val byFields = raw.getAnnotations().find(OpenApiByFields::class.java)
        val namingStrategy = raw.getAnnotations().find(OpenApiNaming::class.java)?.value

        return raw.getProperties().mapNotNull { property ->
            if (!property.includedBy(byFields) || property.transient) return@mapNotNull null
            val annotations = property.annotations
            if (annotations.find(OpenApiIgnore::class.java) != null) return@mapNotNull null

            val customName = annotations.find(OpenApiName::class.java)?.value
            val resolvedName = when {
                customName != null -> customName
                property.accessor == Accessor.GETTER -> property.name.stripGetterPrefix()
                else -> property.name
            }
            val finalName = if (customName == null && namingStrategy != null) translatePropertyName(namingStrategy, resolvedName) else resolvedName

            val isPrimitive = !property.nullable
            val required = annotations.find(OpenApiRequired::class.java) != null ||
                (requireNonNullsByDefault && (annotations.hasNamed("NotNull") || isPrimitive))

            val openApiNullable = annotations.find(OpenApiNullable::class.java)
            val nullable = when {
                openApiNullable != null -> openApiNullable.nullable
                annotations.hasNamed("Nullable") -> true
                else -> false
            }

            val extra = buildMap<String, Any?> {
                annotations.find(OpenApiDescription::class.java)?.let { put("description", it.value) }
            }

            Property(
                name = finalName,
                type = toExperimental(property.type),
                required = required,
                nullable = nullable,
                extra = extra,
            )
        }
    }

    override fun isEnum(type: ClassDefinition): Boolean =
        type.raw.isEnum()

    override fun enumConstants(type: ClassDefinition): List<String>? {
        val raw = type.raw
        if (!raw.isEnum()) return null
        val erasure = raw.source as Class<*>
        val namingStrategy = raw.getAnnotations().find(OpenApiNaming::class.java)?.value
        return raw.getEnumConstants()!!.map { name ->
            val customName = erasure.getField(name).getAnnotation(OpenApiName::class.java)?.value
            when {
                customName != null -> customName
                namingStrategy != null -> translatePropertyName(namingStrategy, name)
                else -> name
            }
        }
    }

    override fun <A : Annotation> annotation(type: ClassDefinition, annotationType: Class<A>): A? =
        type.raw.getAnnotations().find(annotationType)
}

/** `@OpenApiByFields` selection: getters unless `only`, fields at/above the configured visibility, record components always. */
private fun RawProperty.includedBy(byFields: OpenApiByFields?): Boolean =
    when (accessor) {
        Accessor.RECORD_COMPONENT -> true
        Accessor.GETTER -> byFields?.only != true
        Accessor.FIELD -> byFields != null && byFields.value.priority <= visibility.toOpenApi().priority
    }

private fun String.stripGetterPrefix(): String =
    when {
        startsWith("get") -> removePrefix("get").replaceFirstChar { it.lowercase() }
        startsWith("is") -> removePrefix("is").replaceFirstChar { it.lowercase() }
        else -> this
    }

private fun RawVisibility.toOpenApi(): Visibility =
    when (this) {
        RawVisibility.PUBLIC -> Visibility.PUBLIC
        RawVisibility.PROTECTED -> Visibility.PROTECTED
        RawVisibility.PRIVATE -> Visibility.PRIVATE
        RawVisibility.PACKAGE_PRIVATE -> Visibility.DEFAULT
    }

private val ClassDefinition.raw: RawType
    get() = handle as RawType
