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
class JapTypeIntrospector(
    private val types: Types,
    private val elements: Elements,
) : TypeIntrospector {

    override fun introspect(source: Any): ClassDefinition {
        require(source is TypeMirror) { "JapTypeIntrospector expects a javax.lang.model.type.TypeMirror, got ${source::class.java.name}" }
        return classDefinitionOf(source)
    }

    fun introspect(mirror: TypeMirror): ClassDefinition = classDefinitionOf(mirror)

    override fun isEnum(type: ClassDefinition): Boolean =
        typeElementOf(type)?.kind == ElementKind.ENUM

    override fun enumConstants(type: ClassDefinition): List<String>? =
        typeElementOf(type)
            ?.takeIf { it.kind == ElementKind.ENUM }
            ?.enclosedElements
            ?.filter { it.kind == ElementKind.ENUM_CONSTANT }
            ?.map { it.simpleName.toString() }

    override fun annotations(type: ClassDefinition): Annotations =
        annotationsOf(typeElementOf(type))

    override fun properties(type: ClassDefinition): List<PropertyView> {
        val element = typeElementOf(type) ?: return emptyList()

        if (element.kind == ElementKind.RECORD) {
            return element.recordComponents.map { component ->
                PropertyView(component.simpleName.toString(), classDefinitionOf(component.asType()), Accessor.RECORD_COMPONENT, component.asType().nullable(), annotationsOf(component))
            }
        }

        return elements.getAllMembers(element).mapNotNull { member ->
            when {
                member.isGetter() ->
                    PropertyView(member.simpleName.toString(), classDefinitionOf((member as ExecutableElement).returnType), Accessor.GETTER, member.returnType.nullable(), annotationsOf(member))
                member.isInstanceField() ->
                    PropertyView(member.simpleName.toString(), classDefinitionOf((member as VariableElement).asType()), Accessor.FIELD, member.asType().nullable(), annotationsOf(member))
                else -> null
            }
        }
    }

    private fun annotationsOf(element: Element?): Annotations =
        JapAnnotations(element, elements) { classDefinitionOf(it) }

    private fun classDefinitionOf(mirror: TypeMirror, generics: List<ClassDefinition> = emptyList(), structureType: StructureType = DEFAULT): ClassDefinition =
        when (mirror) {
            is TypeVariable -> (mirror.upperBound ?: mirror.lowerBound)?.let { classDefinitionOf(it, generics, structureType) } ?: objectDefinition(structureType)
            is ArrayType -> classDefinitionOf(mirror.componentType, generics, ARRAY)
            is PrimitiveType -> definition(types.boxedClass(mirror).asType(), generics, structureType)
            is DeclaredType -> declared(mirror, generics, structureType)
            else -> types.asElement(mirror)?.asType()?.takeIf { it != mirror }?.let { classDefinitionOf(it, generics, structureType) } ?: objectDefinition(structureType)
        }

    private fun declared(mirror: DeclaredType, generics: List<ClassDefinition>, structureType: StructureType): ClassDefinition {
        val erasure = types.erasure(mirror)
        return when {
            types.isAssignable(erasure, erasureOf("java.util.Map")) ->
                definition(
                    mirror = mirror,
                    generics = listOf(
                        classDefinitionOf(mirror.typeArguments.getOrElse(0) { objectMirror() }),
                        classDefinitionOf(mirror.typeArguments.getOrElse(1) { objectMirror() }),
                    ),
                    structureType = DICTIONARY,
                )
            types.isAssignable(erasure, erasureOf("java.util.Collection")) ->
                classDefinitionOf(mirror.typeArguments.getOrElse(0) { objectMirror() }, generics, ARRAY)
            else ->
                definition(mirror, mirror.typeArguments.map { classDefinitionOf(it) }, structureType)
        }
    }

    private fun definition(mirror: TypeMirror, generics: List<ClassDefinition>, structureType: StructureType): ClassDefinition {
        val element = types.asElement(mirror) as? TypeElement
        val fullName = element?.qualifiedName?.toString() ?: mirror.toString().substringBefore("<")
        return ClassDefinition(
            simpleName = element?.simpleName?.toString() ?: fullName.substringAfterLast('.'),
            fullName = fullName,
            generics = generics,
            structureType = structureType,
            handle = mirror,
        )
    }

    private fun objectDefinition(structureType: StructureType = DEFAULT): ClassDefinition =
        definition(objectMirror(), emptyList(), structureType)

    private fun typeElementOf(type: ClassDefinition): TypeElement? =
        types.asElement(type.handle as TypeMirror) as? TypeElement

    private fun objectMirror(): TypeMirror = elements.getTypeElement("java.lang.Object").asType()
    private fun erasureOf(name: String): TypeMirror = types.erasure(elements.getTypeElement(name).asType())

    private fun TypeMirror.nullable(): Boolean = !kind.isPrimitive

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

internal class JapAnnotations(
    private val element: Element?,
    private val elements: Elements,
    private val resolve: (TypeMirror) -> ClassDefinition,
) : Annotations {
    override fun <A : Annotation> find(annotationType: Class<A>): A? =
        element?.getAnnotation(annotationType)

    override fun hasBySimpleName(simpleName: String): Boolean =
        element?.annotationMirrors?.any { it.annotationType.asElement().simpleName.contentEquals(simpleName) } == true

    override fun <A : Annotation> classValue(annotationType: Class<A>, member: A.() -> KClass<*>): ClassDefinition? {
        val annotation = find(annotationType) ?: return null
        return try {
            elements.getTypeElement(annotation.member().java.name)?.asType()?.let(resolve)
        } catch (mirrored: MirroredTypeException) {
            resolve(mirrored.typeMirror)
        }
    }

    override fun <A : Annotation> classValues(annotationType: Class<A>, member: A.() -> Array<out KClass<*>>): List<ClassDefinition> {
        val annotation = find(annotationType) ?: return emptyList()
        return try {
            annotation.member().mapNotNull { elements.getTypeElement(it.java.name)?.asType()?.let(resolve) }
        } catch (mirrored: MirroredTypesException) {
            mirrored.typeMirrors.map(resolve)
        }
    }

    override fun values(annotationType: Class<out Annotation>): Map<String, Any?>? {
        val mirror = element?.annotationMirrors?.firstOrNull {
            (it.annotationType.asElement() as? TypeElement)?.qualifiedName?.contentEquals(annotationType.name) == true
        } ?: return null

        val visitor = object : SimpleAnnotationValueVisitor8<Any?, Nothing?>() {
            override fun defaultAction(value: Any?, p: Nothing?): Any? = value
            override fun visitType(t: TypeMirror, p: Nothing?): Any = resolve(t)
            override fun visitEnumConstant(constant: VariableElement, p: Nothing?): Any = constant.simpleName.toString()
            override fun visitArray(values: MutableList<out AnnotationValue>, p: Nothing?): Any = values.map { it.accept(this, null) }
        }

        return elements.getElementValuesWithDefaults(mirror).entries
            .associate { (executable, value) -> executable.simpleName.toString() to value.accept(visitor, null) }
    }
}
