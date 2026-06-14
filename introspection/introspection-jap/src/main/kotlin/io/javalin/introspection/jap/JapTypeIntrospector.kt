package io.javalin.introspection.jap

import io.javalin.introspection.Accessor
import io.javalin.introspection.Annotations
import io.javalin.introspection.ClassDefinition
import io.javalin.introspection.PropertyView
import io.javalin.introspection.StructureType
import io.javalin.introspection.StructureType.ARRAY
import io.javalin.introspection.StructureType.DEFAULT
import io.javalin.introspection.StructureType.DICTIONARY
import io.javalin.introspection.TypeIntrospector
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable
import javax.lang.model.util.Elements
import javax.lang.model.util.SimpleAnnotationValueVisitor8
import javax.lang.model.util.Types
import kotlin.reflect.KClass

/** [TypeIntrospector] backed by `javax.lang.model` (Java annotation processing). */
class JapTypeIntrospector(types: Types, elements: Elements) : TypeIntrospector {

    private val env = JapEnv(types, elements)

    override fun introspect(source: Any): ClassDefinition {
        require(source is TypeMirror) { "JapTypeIntrospector expects a javax.lang.model.type.TypeMirror, got ${source::class.java.name}" }
        return env.resolve(source)
    }

    fun introspect(mirror: TypeMirror): ClassDefinition =
        env.resolve(mirror)
}

/** The `javax.lang.model` context + the type-resolution factory shared by the JAP [ClassDefinition]s. */
internal class JapEnv(val types: Types, val elements: Elements) {

    fun resolve(mirror: TypeMirror, generics: List<ClassDefinition> = emptyList(), structureType: StructureType = DEFAULT): ClassDefinition =
        when (mirror) {
            is TypeVariable -> (mirror.upperBound ?: mirror.lowerBound)?.let { resolve(it, generics, structureType) } ?: objectDefinition(structureType)
            is ArrayType -> resolve(mirror.componentType, generics, ARRAY)
            is PrimitiveType -> definition(types.boxedClass(mirror).asType(), generics, structureType)
            is DeclaredType -> declared(mirror, generics, structureType)
            else -> types.asElement(mirror)?.asType()?.takeIf { it != mirror }?.let { resolve(it, generics, structureType) } ?: objectDefinition(structureType)
        }

    fun objectMirror(): TypeMirror =
        elements.getTypeElement("java.lang.Object").asType()

    private fun declared(mirror: DeclaredType, generics: List<ClassDefinition>, structureType: StructureType): ClassDefinition {
        val erasure = types.erasure(mirror)
        return when {
            types.isAssignable(erasure, erasureOf("java.util.Map")) ->
                definition(
                    mirror = mirror,
                    generics = listOf(
                        resolve(mirror.typeArguments.getOrElse(0) { objectMirror() }),
                        resolve(mirror.typeArguments.getOrElse(1) { objectMirror() }),
                    ),
                    structureType = DICTIONARY,
                )
            types.isAssignable(erasure, erasureOf("java.util.Collection")) ->
                resolve(mirror.typeArguments.getOrElse(0) { objectMirror() }, generics, ARRAY)
            else ->
                definition(mirror, mirror.typeArguments.map { resolve(it) }, structureType)
        }
    }

    private fun definition(mirror: TypeMirror, generics: List<ClassDefinition>, structureType: StructureType): ClassDefinition {
        val element = types.asElement(mirror) as? TypeElement
        val fullName = element?.qualifiedName?.toString() ?: mirror.toString().substringBefore("<")
        return JapClassDefinition(
            simpleName = element?.simpleName?.toString() ?: fullName.substringAfterLast('.'),
            fullName = fullName,
            generics = generics,
            structureType = structureType,
            mirror = mirror,
            env = this,
        )
    }

    private fun objectDefinition(structureType: StructureType = DEFAULT): ClassDefinition =
        definition(objectMirror(), emptyList(), structureType)

    private fun erasureOf(name: String): TypeMirror =
        types.erasure(elements.getTypeElement(name).asType())
}

private class JapClassDefinition(
    simpleName: String,
    fullName: String,
    generics: List<ClassDefinition>,
    structureType: StructureType,
    private val mirror: TypeMirror,
    private val env: JapEnv,
) : ClassDefinition(simpleName, fullName, generics, structureType) {

    override fun isEnum(): Boolean =
        element()?.kind == ElementKind.ENUM

    override fun getEnumConstants(): List<String>? =
        element()
            ?.takeIf { it.kind == ElementKind.ENUM }
            ?.enclosedElements
            ?.filter { it.kind == ElementKind.ENUM_CONSTANT }
            ?.map { it.simpleName.toString() }

    override fun getAnnotations(): Annotations =
        JapAnnotations(element(), env)

    override fun getProperties(): List<PropertyView> {
        val element = element() ?: return emptyList()

        if (element.kind == ElementKind.RECORD) {
            return element.recordComponents.map { component ->
                PropertyView(
                    name = component.simpleName.toString(),
                    type = env.resolve(component.asType()),
                    accessor = Accessor.RECORD_COMPONENT,
                    nullable = component.asType().nullable(),
                    annotations = JapAnnotations(component, env),
                )
            }
        }

        return env.elements.getAllMembers(element).mapNotNull { member ->
            when {
                member.isGetter() -> {
                    val getter = member as ExecutableElement
                    PropertyView(
                        name = getter.simpleName.toString(),
                        type = env.resolve(getter.returnType),
                        accessor = Accessor.GETTER,
                        nullable = getter.returnType.nullable(),
                        annotations = JapAnnotations(getter, env),
                    )
                }
                member.isInstanceField() -> {
                    val field = member as VariableElement
                    PropertyView(
                        name = field.simpleName.toString(),
                        type = env.resolve(field.asType()),
                        accessor = Accessor.FIELD,
                        nullable = field.asType().nullable(),
                        annotations = JapAnnotations(field, env),
                    )
                }
                else -> null
            }
        }
    }

    private fun element(): TypeElement? =
        env.types.asElement(mirror) as? TypeElement

    private fun TypeMirror.nullable(): Boolean =
        !kind.isPrimitive

    private fun Element.isGetter(): Boolean {
        if (kind != ElementKind.METHOD || this !is ExecutableElement) return false
        if (Modifier.PUBLIC !in modifiers || Modifier.STATIC in modifiers) return false
        if (parameters.isNotEmpty() || enclosingElement?.toString() == "java.lang.Object") return false
        val name = simpleName.toString()
        return name != "getClass" && (name.startsWith("get") || name.startsWith("is"))
    }

    private fun Element.isInstanceField(): Boolean =
        kind == ElementKind.FIELD && this is VariableElement && Modifier.STATIC !in modifiers
}

private class JapAnnotations(
    private val element: Element?,
    private val env: JapEnv,
) : Annotations {

    override fun <A : Annotation> find(annotationType: Class<A>): A? =
        element?.getAnnotation(annotationType)

    override fun hasNamed(simpleName: String): Boolean =
        element?.annotationMirrors?.any { it.annotationType.asElement().simpleName.contentEquals(simpleName) } == true

    override fun <A : Annotation> resolveType(annotationType: Class<A>, member: A.() -> KClass<*>): ClassDefinition? {
        val annotation = find(annotationType) ?: return null
        return try {
            env.elements.getTypeElement(annotation.member().java.name)?.asType()?.let { env.resolve(it) }
        } catch (mirrored: MirroredTypeException) {
            env.resolve(mirrored.typeMirror)
        }
    }

    override fun <A : Annotation> resolveTypes(annotationType: Class<A>, member: A.() -> Array<out KClass<*>>): List<ClassDefinition> {
        val annotation = find(annotationType) ?: return emptyList()
        return try {
            annotation.member().mapNotNull { kClass -> env.elements.getTypeElement(kClass.java.name)?.asType()?.let { env.resolve(it) } }
        } catch (mirrored: MirroredTypesException) {
            mirrored.typeMirrors.map { env.resolve(it) }
        }
    }

    override fun memberValues(annotationType: Class<out Annotation>): Map<String, Any?>? {
        val mirror = element?.annotationMirrors?.firstOrNull {
            (it.annotationType.asElement() as? TypeElement)?.qualifiedName?.contentEquals(annotationType.name) == true
        } ?: return null

        val visitor = object : SimpleAnnotationValueVisitor8<Any?, Nothing?>() {
            override fun defaultAction(value: Any?, p: Nothing?): Any? = value
            override fun visitType(t: TypeMirror, p: Nothing?): Any = env.resolve(t)
            override fun visitEnumConstant(constant: VariableElement, p: Nothing?): Any = constant.simpleName.toString()
            override fun visitArray(values: MutableList<out AnnotationValue>, p: Nothing?): Any = values.map { it.accept(this, null) }
        }

        return env.elements.getElementValuesWithDefaults(mirror).entries
            .associate { (executable, value) -> executable.simpleName.toString() to value.accept(visitor, null) }
    }
}
