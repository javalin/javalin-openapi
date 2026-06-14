package io.javalin.introspection.runtime

import io.javalin.introspection.Accessor
import io.javalin.introspection.Annotations
import io.javalin.introspection.ClassDefinition
import io.javalin.introspection.PropertyView
import io.javalin.introspection.StructureType
import io.javalin.introspection.StructureType.ARRAY
import io.javalin.introspection.StructureType.DEFAULT
import io.javalin.introspection.StructureType.DICTIONARY
import io.javalin.introspection.TypeIntrospector
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass

/** [TypeIntrospector] backed by reflection ([java.lang.reflect.Type]). */
class ReflectionTypeIntrospector : TypeIntrospector {

    override fun introspect(source: Any): ClassDefinition {
        require(source is Type) { "ReflectionTypeIntrospector expects a java.lang.reflect.Type, got ${source::class.java.name}" }
        return reflect(source)
    }

    fun introspect(type: Type): ClassDefinition =
        reflect(type)
}

private fun reflect(type: Type, generics: List<ClassDefinition> = emptyList(), structureType: StructureType = DEFAULT): ClassDefinition =
    when (type) {
        is GenericArrayType -> reflect(type.genericComponentType, generics, ARRAY)
        is WildcardType -> reflect(type.upperBounds.firstOrNull() ?: Any::class.java, generics, structureType)
        is ParameterizedType -> parameterized(type, generics, structureType)
        is Class<*> -> raw(type, generics, structureType)
        else -> definition(Any::class.java, emptyList(), structureType)
    }

private fun raw(clazz: Class<*>, generics: List<ClassDefinition>, structureType: StructureType): ClassDefinition =
    when {
        clazz.isArray -> reflect(clazz.componentType, generics, ARRAY)
        clazz.isPrimitive -> definition(clazz.kotlin.javaObjectType, generics, structureType)
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
                    reflect(arguments.getOrElse(0) { Any::class.java }),
                    reflect(arguments.getOrElse(1) { Any::class.java }),
                ),
                structureType = DICTIONARY,
            )
        Collection::class.java.isAssignableFrom(erasure) ->
            reflect(arguments.getOrElse(0) { Any::class.java }, generics, ARRAY)
        else ->
            definition(erasure, arguments.map { reflect(it) }, structureType)
    }
}

private fun definition(erasure: Class<*>, generics: List<ClassDefinition>, structureType: StructureType): ClassDefinition =
    ReflectionClassDefinition(
        simpleName = erasure.simpleName.ifEmpty { erasure.name.substringAfterLast('.') },
        fullName = erasure.canonicalName ?: erasure.name,
        generics = generics,
        structureType = structureType,
        erasure = erasure,
    )

private fun objectDefinition(structureType: StructureType = DEFAULT): ClassDefinition =
    definition(Any::class.java, emptyList(), structureType)

private class ReflectionClassDefinition(
    simpleName: String,
    fullName: String,
    generics: List<ClassDefinition>,
    structureType: StructureType,
    private val erasure: Class<*>,
) : ClassDefinition(simpleName, fullName, generics, structureType) {

    override fun isEnum(): Boolean =
        erasure.isEnum

    override fun getEnumConstants(): List<String>? =
        erasure.takeIf { it.isEnum }?.enumConstants?.map { (it as Enum<*>).name }

    override fun getProperties(): List<PropertyView> =
        collectMembers(erasure).map { member ->
            PropertyView(
                name = member.rawName,
                type = reflect(member.genericType),
                accessor = member.accessor,
                nullable = (member.genericType as? Class<*>)?.isPrimitive != true,
                annotations = ReflectionAnnotations(member.sources),
            )
        }

    override fun getAnnotations(): Annotations =
        ReflectionAnnotations(listOf(erasure))
}

private fun collectMembers(clazz: Class<*>): List<Member> {
    if (clazz.isRecord) {
        return clazz.recordComponents.map { component ->
            val backingField = runCatching { clazz.getDeclaredField(component.name) }.getOrNull()
            Member(component.name, component.genericType, Accessor.RECORD_COMPONENT, listOfNotNull(component.accessor, backingField, component))
        }
    }

    val members = mutableListOf<Member>()

    for (method in clazz.methods) {
        if (Modifier.isStatic(method.modifiers) || method.isBridge || method.isSynthetic) continue
        if (method.parameterCount != 0 || method.declaringClass == Any::class.java) continue
        if (method.name == "getClass") continue
        if (!method.name.startsWith("get") && !method.name.startsWith("is")) continue
        members += Member(method.name, method.genericReturnType, Accessor.GETTER, listOf(method))
    }

    for (field in declaredFieldsHierarchy(clazz)) {
        if (Modifier.isStatic(field.modifiers)) continue
        members += Member(field.name, field.genericType, Accessor.FIELD, listOf(field))
    }

    return members
}

private fun declaredFieldsHierarchy(clazz: Class<*>): List<Field> {
    val fields = mutableListOf<Field>()
    var current: Class<*>? = clazz
    while (current != null && current != Any::class.java) {
        // private fields aren't inherited members, so include them only for the leaf class (matches getAllMembers)
        current.declaredFields.filterTo(fields) { current == clazz || !Modifier.isPrivate(it.modifiers) }
        current = current.superclass
    }
    return fields
}

private class Member(
    val rawName: String,
    val genericType: Type,
    val accessor: Accessor,
    val sources: List<AnnotatedElement>,
)

private class ReflectionAnnotations(private val sources: List<AnnotatedElement>) : Annotations {

    override fun <A : Annotation> find(annotationType: Class<A>): A? =
        sources.firstNotNullOfOrNull { it.getAnnotation(annotationType) }

    override fun hasNamed(simpleName: String): Boolean =
        sources.any { source -> source.annotations.any { it.annotationClass.simpleName == simpleName } }

    override fun <A : Annotation> resolveType(annotationType: Class<A>, member: A.() -> KClass<*>): ClassDefinition? =
        find(annotationType)?.let { reflect(it.member().java) }

    override fun <A : Annotation> resolveTypes(annotationType: Class<A>, member: A.() -> Array<out KClass<*>>): List<ClassDefinition> =
        find(annotationType)?.member()?.map { reflect(it.java) } ?: emptyList()

    override fun memberValues(annotationType: Class<out Annotation>): Map<String, Any?>? {
        val annotation = find(annotationType) ?: return null
        return annotationType.declaredMethods.associate { it.name to normalize(it.invoke(annotation)) }
    }

    private fun normalize(value: Any?): Any? =
        when (value) {
            is Class<*> -> reflect(value)
            is Enum<*> -> value.name
            is Array<*> -> value.map { normalize(it) }
            else -> value
        }
}
