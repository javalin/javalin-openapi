package io.javalin.introspection.runtime

import io.javalin.introspection.Accessor
import io.javalin.introspection.AnnotationProjection
import io.javalin.introspection.AnnotationSet
import io.javalin.introspection.ClassDefinition
import io.javalin.introspection.EnumConstant
import io.javalin.introspection.InternalIntrospectionApi
import io.javalin.introspection.PropertyProjection
import io.javalin.introspection.StructureType
import io.javalin.introspection.StructureType.ARRAY
import io.javalin.introspection.StructureType.DEFAULT
import io.javalin.introspection.StructureType.DICTIONARY
import io.javalin.introspection.TypeIntrospector
import io.javalin.introspection.MemberVisibility
import io.javalin.introspection.isGetterName
import io.javalin.introspection.propertyName
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Array as JavaArray
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

class ReflectionTypeIntrospector : TypeIntrospector {

    override fun introspect(source: Any): ClassDefinition {
        require(source is Type) { "ReflectionTypeIntrospector expects a java.lang.reflect.Type, got ${source::class.java.name}" }
        return reflect(source)
    }
}

private fun reflect(
    type: Type,
    generics: List<ClassDefinition> = emptyList(),
    structureType: StructureType = DEFAULT,
    visitingTypeVariables: Set<TypeVariable<*>> = emptySet(),
): ClassDefinition =
    when (type) {
        is GenericArrayType ->
            reflect(
                type = type.genericComponentType,
                generics = generics,
                structureType = ARRAY,
                visitingTypeVariables = visitingTypeVariables,
            )
        is WildcardType ->
            reflect(
                type = type.upperBounds.firstOrNull() ?: Any::class.java,
                generics = generics,
                structureType = structureType,
                visitingTypeVariables = visitingTypeVariables,
            )
        is TypeVariable<*> ->
            when {
                type in visitingTypeVariables -> objectDefinition(structureType)
                else ->
                    reflect(
                        type = type.bounds.firstOrNull() ?: Any::class.java,
                        generics = generics,
                        structureType = structureType,
                        visitingTypeVariables = visitingTypeVariables + type,
                    )
            }
        is ParameterizedType -> parameterized(
            type = type,
            generics = generics,
            structureType = structureType,
            visitingTypeVariables = visitingTypeVariables,
        )
        is Class<*> -> raw(
            clazz = type,
            generics = generics,
            structureType = structureType,
            visitingTypeVariables = visitingTypeVariables,
        )
        else -> definition(erasure = Any::class.java, generics = emptyList(), structureType = structureType)
    }

private fun raw(
    clazz: Class<*>,
    generics: List<ClassDefinition>,
    structureType: StructureType,
    visitingTypeVariables: Set<TypeVariable<*>>,
): ClassDefinition =
    when {
        clazz.isArray -> reflect(
            type = clazz.componentType,
            generics = generics,
            structureType = ARRAY,
            visitingTypeVariables = visitingTypeVariables,
        )
        clazz.isPrimitive -> definition(erasure = clazz.kotlin.javaObjectType, generics = generics, structureType = structureType)
        Map::class.java.isAssignableFrom(clazz) ->
            definition(
                erasure = clazz,
                generics = listOf(objectDefinition(), objectDefinition()),
                structureType = DICTIONARY,
            )
        Collection::class.java.isAssignableFrom(clazz) -> objectDefinition(ARRAY)
        else -> definition(erasure = clazz, generics = generics, structureType = structureType)
    }

private fun parameterized(
    type: ParameterizedType,
    generics: List<ClassDefinition>,
    structureType: StructureType,
    visitingTypeVariables: Set<TypeVariable<*>>,
): ClassDefinition {
    val erasure = type.rawType as Class<*>
    val arguments = type.actualTypeArguments
    return when {
        Map::class.java.isAssignableFrom(erasure) -> {
            val keyType = reflect(arguments.getOrElse(0) { Any::class.java }, visitingTypeVariables = visitingTypeVariables)
            val valueType = reflect(arguments.getOrElse(1) { Any::class.java }, visitingTypeVariables = visitingTypeVariables)
            definition(
                erasure = erasure,
                generics = listOf(keyType, valueType),
                structureType = DICTIONARY,
            )
        }
        Collection::class.java.isAssignableFrom(erasure) ->
            reflect(
                type = arguments.getOrElse(0) { Any::class.java },
                generics = generics,
                structureType = ARRAY,
                visitingTypeVariables = visitingTypeVariables,
            )
        else -> {
            val resolvedGenerics = arguments.map {
                reflect(it, visitingTypeVariables = visitingTypeVariables)
            }
            definition(erasure = erasure, generics = resolvedGenerics, structureType = structureType)
        }
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
    definition(erasure = Any::class.java, generics = emptyList(), structureType = structureType)

private class ReflectionClassDefinition(
    simpleName: String,
    fullName: String,
    generics: List<ClassDefinition>,
    structureType: StructureType,
    private val erasure: Class<*>,
) : ClassDefinition(
    simpleName = simpleName,
    fullName = fullName,
    generics = generics,
    structureType = structureType,
) {

    @InternalIntrospectionApi
    override val source: Any = erasure

    override fun isEnum(): Boolean =
        erasure.isEnum

    override fun getEnumConstants(): List<EnumConstant> {
        if (!erasure.isEnum) {
            return emptyList()
        }

        return erasure.enumConstants.map { constant ->
            val name = (constant as Enum<*>).name
            val field = runCatching { erasure.getDeclaredField(name) }.getOrNull()
            EnumConstant(
                name = name,
                annotations = ReflectionAnnotations(listOfNotNull(field)),
            )
        }
    }

    override fun getProperties(): List<PropertyProjection> =
        collectMembers(erasure).map { member ->
            PropertyProjection(
                name = when {
                    member.accessor == Accessor.GETTER -> propertyName(member.name)
                    else -> member.name
                },
                type = reflect(member.genericType),
                accessor = member.accessor,
                nullable = (member.genericType as? Class<*>)?.isPrimitive != true,
                visibility = member.visibility,
                transient = member.transient,
                source = member.source,
                annotations = ReflectionAnnotations(member.sources),
            )
        }

    override fun getAnnotations(): AnnotationSet = ReflectionAnnotations(listOf(erasure))
}

private fun collectMembers(clazz: Class<*>): List<Member> {
    if (clazz.isRecord) {
        return clazz.recordComponents.map { component ->
            val backingField = runCatching { clazz.getDeclaredField(component.name) }.getOrNull()
            Member(
                name = component.name,
                genericType = component.genericType,
                accessor = Accessor.RECORD_COMPONENT,
                visibility = MemberVisibility.PUBLIC,
                transient = false,
                source = component.accessor,
                sources = listOfNotNull(component.accessor, backingField, component),
            )
        }
    }

    val members = mutableListOf<Member>()
    val getterNames = mutableSetOf<String>()

    for (method in clazz.methods) {
        if (Modifier.isStatic(method.modifiers) || method.isBridge || method.isSynthetic) continue
        if (method.parameterCount != 0 || method.declaringClass == Any::class.java) continue
        if (method.returnType == Void.TYPE || !isGetterName(method.name)) continue
        if (getterNames.add(method.name)) {
            members += method.toMember()
        }
    }

    // clazz.methods is public-only, so a second pass picks up inherited protected/package-private getters (jap parity)
    for (method in nonPublicGettersHierarchy(clazz)) {
        if (getterNames.add(method.name)) {
            members += method.toMember()
        }
    }

    for (field in declaredFieldsHierarchy(clazz)) {
        if (Modifier.isStatic(field.modifiers) || field.isSynthetic) continue
        members += Member(
            name = field.name,
            genericType = field.genericType,
            accessor = Accessor.FIELD,
            visibility = visibilityOf(field.modifiers),
            transient = Modifier.isTransient(field.modifiers),
            source = field,
            sources = listOf(field),
        )
    }

    return members
}

private class Member(
    val name: String,
    val genericType: Type,
    val accessor: Accessor,
    val visibility: MemberVisibility,
    val transient: Boolean,
    val source: AnnotatedElement,
    val sources: List<AnnotatedElement>,
)

private fun Method.toMember(): Member =
    Member(
        name = name,
        genericType = genericReturnType,
        accessor = Accessor.GETTER,
        visibility = visibilityOf(modifiers),
        transient = false,
        source = this,
        sources = listOf(this),
    )

private fun visibilityOf(modifiers: Int): MemberVisibility =
    when {
        Modifier.isPublic(modifiers) -> MemberVisibility.PUBLIC
        Modifier.isProtected(modifiers) -> MemberVisibility.PROTECTED
        Modifier.isPrivate(modifiers) -> MemberVisibility.PRIVATE
        else -> MemberVisibility.PACKAGE_PRIVATE
    }

private fun declaredFieldsHierarchy(clazz: Class<*>): List<Field> {
    val fields = mutableListOf<Field>()
    var current: Class<*>? = clazz
    while (current != null && current != Any::class.java) {
        // only members [clazz] actually inherits, matching JAP's getAllMembers: private stays in the leaf,
        // package-private is inherited only within the same package
        current.declaredFields.filterTo(fields) { field ->
            val modifiers = field.modifiers
            when {
                current == clazz -> true
                Modifier.isPrivate(modifiers) -> false
                Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) -> true
                else -> field.declaringClass.packageName == clazz.packageName
            }
        }
        current = current.superclass
    }
    return fields
}

private fun nonPublicGettersHierarchy(clazz: Class<*>): List<Method> {
    val getters = mutableListOf<Method>()
    var current: Class<*>? = clazz
    while (current != null && current != Any::class.java) {
        for (method in current.declaredMethods) {
            val modifiers = method.modifiers
            if (Modifier.isStatic(modifiers) || Modifier.isPublic(modifiers) || method.isBridge || method.isSynthetic) continue
            if (method.parameterCount != 0 || method.returnType == Void.TYPE || !isGetterName(method.name)) continue
            val inherited = when {
                current == clazz -> true
                Modifier.isPrivate(modifiers) -> false
                Modifier.isProtected(modifiers) -> true
                else -> method.declaringClass.packageName == clazz.packageName
            }
            if (inherited) getters += method
        }
        current = current.superclass
    }
    return getters
}

private class ReflectionAnnotations(private val sources: List<AnnotatedElement>) : AnnotationSet {

    override fun contains(simpleName: String): Boolean =
        sources.any { source -> source.annotations.any { it.annotationClass.simpleName == simpleName } }

    override fun find(type: Class<out Annotation>): AnnotationProjection? =
        sources.firstNotNullOfOrNull { it.getAnnotation(type) }?.let { ReflectionAnnotationProjection(it) }

    override fun findAll(type: Class<out Annotation>): List<AnnotationProjection> =
        sources.flatMap { it.getAnnotationsByType(type).toList() }.map { ReflectionAnnotationProjection(it) }

    override fun all(): List<AnnotationProjection> =
        sources.flatMap { it.annotations.toList() }.distinctBy { it.annotationClass }.map { ReflectionAnnotationProjection(it) }
}

private class ReflectionAnnotationProjection(private val annotation: Annotation) : AnnotationProjection {

    override val simpleName: String
        get() = annotation.annotationClass.java.simpleName

    override val metadata: AnnotationSet
        get() = ReflectionAnnotations(listOf(annotation.annotationClass.java))

    override val values: Map<String, Any?>
        get() = annotationToMap(annotation)
}

private fun annotationToMap(annotation: Annotation): Map<String, Any?> =
    annotation.annotationClass.java.declaredMethods.associate {
        it.trySetAccessible()
        it.name to normalize(it.invoke(annotation))
    }

private fun normalize(value: Any?): Any? =
    when {
        value is Class<*> -> reflect(value)
        value is Enum<*> -> value.name
        value is Annotation -> annotationToMap(value)
        value is Array<*> -> value.map { normalize(it) }
        value != null && value::class.java.isArray ->
            (0 until JavaArray.getLength(value)).map { normalize(JavaArray.get(value, it)) }
        else -> value
    }
