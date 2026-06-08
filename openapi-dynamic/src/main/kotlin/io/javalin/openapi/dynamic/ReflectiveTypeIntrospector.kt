package io.javalin.openapi.dynamic

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
import io.javalin.openapi.experimental.StructureType.ARRAY
import io.javalin.openapi.experimental.StructureType.DEFAULT
import io.javalin.openapi.experimental.StructureType.DICTIONARY
import io.javalin.openapi.experimental.TypeIntrospector
import io.javalin.openapi.experimental.processor.generators.Property
import io.javalin.openapi.experimental.processor.generators.translatePropertyName
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

/** Runtime-reflection [TypeIntrospector] over [java.lang.reflect.Type]. */
class ReflectiveTypeIntrospector(
    private val requireNonNullsByDefault: Boolean = true,
) : TypeIntrospector {

    override fun introspect(nativeType: Any): ClassDefinition {
        require(nativeType is Type) {
            "ReflectiveTypeIntrospector expects a java.lang.reflect.Type, got ${nativeType::class.java.name}"
        }
        return classDefinitionOf(nativeType)
    }

    fun introspect(type: Type): ClassDefinition = classDefinitionOf(type)

    private fun classDefinitionOf(
        type: Type,
        generics: List<ClassDefinition> = emptyList(),
        structureType: StructureType = DEFAULT,
    ): ClassDefinition =
        when (type) {
            is GenericArrayType -> classDefinitionOf(type.genericComponentType, generics, ARRAY)
            is WildcardType -> classDefinitionOf(type.upperBounds.firstOrNull() ?: Any::class.java, generics, structureType)
            is ParameterizedType -> parameterized(type, generics, structureType)
            is Class<*> -> raw(type, generics, structureType)
            else -> objectDefinition(structureType)
        }

    private fun raw(clazz: Class<*>, generics: List<ClassDefinition>, structureType: StructureType): ClassDefinition =
        when {
            clazz.isArray -> classDefinitionOf(clazz.componentType, generics, ARRAY)
            clazz.isPrimitive -> definition(box(clazz), generics, structureType)
            Map::class.java.isAssignableFrom(clazz) -> definition(clazz, listOf(objectDefinition(), objectDefinition()), DICTIONARY)
            Collection::class.java.isAssignableFrom(clazz) -> objectDefinition(ARRAY)
            else -> definition(clazz, generics, structureType)
        }

    private fun parameterized(type: ParameterizedType, generics: List<ClassDefinition>, structureType: StructureType): ClassDefinition {
        val erasure = type.rawType as Class<*>
        val arguments = type.actualTypeArguments
        return when {
            Map::class.java.isAssignableFrom(erasure) ->
                definition(
                    erasure = erasure,
                    generics = listOf(
                        classDefinitionOf(arguments.getOrElse(0) { Any::class.java }),
                        classDefinitionOf(arguments.getOrElse(1) { Any::class.java }),
                    ),
                    structureType = DICTIONARY,
                    fullType = type,
                )
            Collection::class.java.isAssignableFrom(erasure) ->
                classDefinitionOf(arguments.getOrElse(0) { Any::class.java }, generics, ARRAY)
            else ->
                definition(
                    erasure = erasure,
                    generics = arguments.map { classDefinitionOf(it) },
                    structureType = structureType,
                    fullType = type,
                )
        }
    }

    private fun definition(
        erasure: Class<*>,
        generics: List<ClassDefinition>,
        structureType: StructureType,
        fullType: Type = erasure,
    ): ClassDefinition {
        val customName = erasure.getAnnotation(OpenApiName::class.java)?.value
        val packageName = erasure.`package`?.name?.takeIf { it.isNotEmpty() }
        return ClassDefinition(
            simpleName = customName ?: erasure.simpleName.ifEmpty { erasure.name.substringAfterLast('.') },
            fullName = when {
                customName != null -> if (packageName == null) customName else "$packageName.$customName"
                else -> erasure.canonicalName ?: erasure.name
            },
            generics = generics,
            structureType = structureType,
            handle = ReflectionHandle(erasure, fullType),
        )
    }

    private fun objectDefinition(structureType: StructureType = DEFAULT): ClassDefinition =
        definition(Any::class.java, emptyList(), structureType)

    override fun properties(type: ClassDefinition): List<Property> {
        val clazz = type.reflectedClass
        val byFields = clazz.getAnnotation(OpenApiByFields::class.java)
        val namingStrategy = clazz.getAnnotation(OpenApiNaming::class.java)?.value

        return collectMembers(clazz, byFields).mapNotNull { member ->
            if (member.annotation(OpenApiIgnore::class.java) != null) return@mapNotNull null
            if (member.isField && Modifier.isTransient(member.modifiers)) return@mapNotNull null

            val customName = member.annotation(OpenApiName::class.java)?.value
            val resolvedName = when {
                customName != null -> customName
                member.isField || member.isRecordComponent -> member.rawName
                member.rawName.startsWith("get") -> member.rawName.removePrefix("get").replaceFirstChar { it.lowercase() }
                member.rawName.startsWith("is") -> member.rawName.removePrefix("is").replaceFirstChar { it.lowercase() }
                else -> return@mapNotNull null
            }
            val finalName = if (customName == null && namingStrategy != null) translatePropertyName(namingStrategy, resolvedName) else resolvedName

            val isPrimitive = (member.genericType as? Class<*>)?.isPrimitive == true
            val isNotNull = when {
                member.hasAnnotationNamed("NotNull") -> true
                isPrimitive -> true
                member.hasAnnotationNamed("Nullable") -> false
                else -> false
            }
            val required = member.annotation(OpenApiRequired::class.java) != null || (requireNonNullsByDefault && isNotNull)

            val openApiNullable = member.annotation(OpenApiNullable::class.java)
            val nullable = when {
                openApiNullable != null -> openApiNullable.nullable
                member.hasAnnotationNamed("Nullable") -> true
                else -> false
            }

            val extra = buildMap<String, Any?> {
                member.annotation(OpenApiDescription::class.java)?.let { put("description", it.value) }
            }

            Property(
                name = finalName,
                type = classDefinitionOf(member.genericType),
                required = required,
                nullable = nullable,
                extra = extra,
            )
        }
    }

    private fun collectMembers(clazz: Class<*>, byFields: OpenApiByFields?): List<Member> {
        if (clazz.isRecord) {
            return clazz.recordComponents.map { component ->
                val backingField = runCatching { clazz.getDeclaredField(component.name) }.getOrNull()
                Member(
                    rawName = component.name,
                    genericType = component.genericType,
                    modifiers = 0,
                    sources = listOfNotNull(component.accessor, backingField, component),
                    isField = false,
                    isRecordComponent = true,
                )
            }
        }

        val members = mutableListOf<Member>()

        if (byFields?.only != true) {
            for (method in clazz.methods) {
                if (Modifier.isStatic(method.modifiers) || method.isBridge || method.isSynthetic) continue
                if (method.parameterCount != 0 || method.declaringClass == Any::class.java) continue
                if (method.name == "getClass") continue
                if (!method.name.startsWith("get") && !method.name.startsWith("is")) continue
                members += Member(method.name, method.genericReturnType, method.modifiers, listOf(method), isField = false, isRecordComponent = false)
            }
        }

        if (byFields != null) {
            for (field in declaredFieldsHierarchy(clazz)) {
                if (Modifier.isStatic(field.modifiers)) continue
                if (byFields.value.priority > visibilityOf(field.modifiers).priority) continue
                members += Member(field.name, field.genericType, field.modifiers, listOf(field), isField = true, isRecordComponent = false)
            }
        }

        return members
    }

    override fun isEnum(type: ClassDefinition): Boolean =
        type.reflectedClass.isEnum

    override fun enumConstants(type: ClassDefinition): List<String>? {
        val clazz = type.reflectedClass
        if (!clazz.isEnum) return null
        val namingStrategy = clazz.getAnnotation(OpenApiNaming::class.java)?.value
        return clazz.enumConstants.map { constant ->
            val name = (constant as Enum<*>).name
            val customName = clazz.getField(name).getAnnotation(OpenApiName::class.java)?.value
            when {
                customName != null -> customName
                namingStrategy != null -> translatePropertyName(namingStrategy, name)
                else -> name
            }
        }
    }

    override fun <A : Annotation> annotation(type: ClassDefinition, annotationType: Class<A>): A? =
        type.reflectedClass.getAnnotation(annotationType)

    private companion object {
        private fun box(primitive: Class<*>): Class<*> = when (primitive) {
            Int::class.javaPrimitiveType -> Int::class.javaObjectType
            Long::class.javaPrimitiveType -> Long::class.javaObjectType
            Short::class.javaPrimitiveType -> Short::class.javaObjectType
            Byte::class.javaPrimitiveType -> Byte::class.javaObjectType
            Char::class.javaPrimitiveType -> Char::class.javaObjectType
            Boolean::class.javaPrimitiveType -> Boolean::class.javaObjectType
            Float::class.javaPrimitiveType -> Float::class.javaObjectType
            Double::class.javaPrimitiveType -> Double::class.javaObjectType
            Void.TYPE -> Void::class.javaObjectType
            else -> primitive
        }

        private fun declaredFieldsHierarchy(clazz: Class<*>): List<Field> {
            val fields = mutableListOf<Field>()
            var current: Class<*>? = clazz
            while (current != null && current != Any::class.java) {
                fields += current.declaredFields
                current = current.superclass
            }
            return fields
        }

        private fun visibilityOf(modifiers: Int): Visibility = when {
            Modifier.isPublic(modifiers) -> Visibility.PUBLIC
            Modifier.isProtected(modifiers) -> Visibility.PROTECTED
            Modifier.isPrivate(modifiers) -> Visibility.PRIVATE
            else -> Visibility.DEFAULT
        }
    }

    private class Member(
        val rawName: String,
        val genericType: Type,
        val modifiers: Int,
        val sources: List<AnnotatedElement>,
        val isField: Boolean,
        val isRecordComponent: Boolean,
    ) {
        fun <A : Annotation> annotation(annotationType: Class<A>): A? =
            sources.firstNotNullOfOrNull { it.getAnnotation(annotationType) }

        fun hasAnnotationNamed(simpleName: String): Boolean =
            sources.any { source -> source.annotations.any { it.annotationClass.simpleName == simpleName } }
    }
}
