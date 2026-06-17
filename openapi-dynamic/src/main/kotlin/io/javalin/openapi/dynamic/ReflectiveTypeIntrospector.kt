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
        val erasure = raw.source as Class<*>
        val byFields = erasure.getAnnotation(OpenApiByFields::class.java)
        val namingStrategy = erasure.getAnnotation(OpenApiNaming::class.java)?.value

        return raw.getProperties().mapNotNull { property ->
            if (!property.includedBy(byFields) || property.transient) return@mapNotNull null
            val annotations = property.annotations
            if (annotations.has(OpenApiIgnore::class.java)) return@mapNotNull null

            val customName = annotations.memberValues(OpenApiName::class.java)?.get("value") as? String
            val resolvedName = customName ?: property.name
            val finalName = if (customName == null && namingStrategy != null) translatePropertyName(namingStrategy, resolvedName) else resolvedName

            val isPrimitive = !property.nullable
            val required = annotations.has(OpenApiRequired::class.java) ||
                (requireNonNullsByDefault && (annotations.hasNamed("NotNull") || isPrimitive))

            val explicitNullable = annotations.memberValues(OpenApiNullable::class.java)?.get("nullable") as? Boolean
            val nullable = when {
                explicitNullable != null -> explicitNullable
                annotations.hasNamed("Nullable") -> true
                else -> false
            }

            val extra = buildMap<String, Any?> {
                (annotations.memberValues(OpenApiDescription::class.java)?.get("value") as? String)?.let { put("description", it) }
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
        val namingStrategy = erasure.getAnnotation(OpenApiNaming::class.java)?.value
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
        (type.raw.source as Class<*>).getAnnotation(annotationType)
}

/** `@OpenApiByFields` selection: getter-backed properties by default; with the annotation, fields at/above the configured visibility (and getters too unless `only`). */
private fun RawProperty.includedBy(byFields: OpenApiByFields?): Boolean {
    if (byFields == null) return Accessor.GETTER in accessors
    val fieldVisible = Accessor.FIELD in accessors && byFields.value.priority <= visibility.toOpenApi().priority
    return if (byFields.only) fieldVisible else Accessor.GETTER in accessors || fieldVisible
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
