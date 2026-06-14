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
import io.javalin.introspection.Visibility
import io.javalin.introspection.isGetterName
import javax.lang.model.element.AnnotationMirror
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
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable
import javax.lang.model.type.WildcardType
import javax.lang.model.util.Elements
import javax.lang.model.util.SimpleAnnotationValueVisitor8
import javax.lang.model.util.Types
import kotlin.reflect.KClass

/** [TypeIntrospector] backed by `javax.lang.model` (Java annotation processing). */
class JapTypeIntrospector(private val types: Types, private val elements: Elements) : TypeIntrospector {

    override fun introspect(source: Any): ClassDefinition {
        require(source is TypeMirror) { "JapTypeIntrospector expects a javax.lang.model.type.TypeMirror, got ${source::class.java.name}" }
        return resolve(source)
    }

    private fun resolve(mirror: TypeMirror, generics: List<ClassDefinition> = emptyList(), structureType: StructureType = DEFAULT): ClassDefinition =
        when (mirror) {
            is TypeVariable -> (mirror.upperBound ?: mirror.lowerBound)?.let { resolve(it, generics, structureType) } ?: objectDefinition(structureType)
            is WildcardType -> resolve(mirror.extendsBound ?: objectMirror(), generics, structureType)
            is ArrayType -> resolve(mirror.componentType, generics, ARRAY)
            is PrimitiveType -> definition(types.boxedClass(mirror).asType(), generics, structureType)
            is DeclaredType -> declared(mirror, generics, structureType)
            else -> types.asElement(mirror)?.asType()?.takeIf { it != mirror }?.let { resolve(it, generics, structureType) } ?: objectDefinition(structureType)
        }

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
        return Definition(
            simpleName = element?.simpleName?.toString() ?: fullName.substringAfterLast('.'),
            fullName = fullName,
            generics = generics,
            structureType = structureType,
            mirror = mirror,
        )
    }

    private fun objectDefinition(structureType: StructureType = DEFAULT): ClassDefinition =
        definition(objectMirror(), emptyList(), structureType)

    private fun objectMirror(): TypeMirror =
        elements.getTypeElement("java.lang.Object").asType()

    private fun erasureOf(name: String): TypeMirror =
        types.erasure(elements.getTypeElement(name).asType())

    private inner class Definition(
        simpleName: String,
        fullName: String,
        generics: List<ClassDefinition>,
        structureType: StructureType,
        private val mirror: TypeMirror,
    ) : ClassDefinition(simpleName, fullName, generics, structureType) {

        override val source: Any
            get() = mirror

        override fun isEnum(): Boolean =
            typeElement()?.kind == ElementKind.ENUM

        override fun getEnumConstants(): List<String>? =
            typeElement()
                ?.takeIf { it.kind == ElementKind.ENUM }
                ?.enclosedElements
                ?.filter { it.kind == ElementKind.ENUM_CONSTANT }
                ?.map { it.simpleName.toString() }

        override fun getAnnotations(): Annotations =
            AnnotationsView(typeElement())

        override fun getProperties(): List<PropertyView> {
            val element = typeElement() ?: return emptyList()

            if (element.kind == ElementKind.RECORD) {
                return element.recordComponents.map { component ->
                    PropertyView(
                        name = component.simpleName.toString(),
                        type = resolve(component.asType()),
                        accessor = Accessor.RECORD_COMPONENT,
                        nullable = component.asType().nullable(),
                        visibility = Visibility.PUBLIC,
                        transient = false,
                        annotations = AnnotationsView(component),
                    )
                }
            }

            return elements.getAllMembers(element).mapNotNull { member ->
                when {
                    member.isGetter() -> {
                        val getter = member as ExecutableElement
                        PropertyView(
                            name = getter.simpleName.toString(),
                            type = resolve(getter.returnType),
                            accessor = Accessor.GETTER,
                            nullable = getter.returnType.nullable(),
                            visibility = getter.visibility(),
                            transient = false,
                            annotations = AnnotationsView(getter),
                        )
                    }
                    member.isInstanceField() -> {
                        val field = member as VariableElement
                        PropertyView(
                            name = field.simpleName.toString(),
                            type = resolve(field.asType()),
                            accessor = Accessor.FIELD,
                            nullable = field.asType().nullable(),
                            visibility = field.visibility(),
                            transient = Modifier.TRANSIENT in field.modifiers,
                            annotations = AnnotationsView(field),
                        )
                    }
                    else -> null
                }
            }
        }

        private fun typeElement(): TypeElement? =
            types.asElement(mirror) as? TypeElement

        private fun TypeMirror.nullable(): Boolean =
            !kind.isPrimitive

        private fun Element.isGetter(): Boolean {
            if (kind != ElementKind.METHOD || this !is ExecutableElement) return false
            if (Modifier.PUBLIC !in modifiers || Modifier.STATIC in modifiers) return false
            if (parameters.isNotEmpty() || enclosingElement?.toString() == "java.lang.Object") return false
            if (returnType.kind == TypeKind.VOID) return false
            return isGetterName(simpleName.toString())
        }

        private fun Element.isInstanceField(): Boolean =
            kind == ElementKind.FIELD && this is VariableElement && Modifier.STATIC !in modifiers

        private fun Element.visibility(): Visibility =
            when {
                Modifier.PUBLIC in modifiers -> Visibility.PUBLIC
                Modifier.PROTECTED in modifiers -> Visibility.PROTECTED
                Modifier.PRIVATE in modifiers -> Visibility.PRIVATE
                else -> Visibility.PACKAGE_PRIVATE
            }
    }

    private inner class AnnotationsView(private val element: Element?) : Annotations {

        override fun <A : Annotation> find(annotationType: Class<A>): A? =
            element?.getAnnotation(annotationType)

        override fun hasNamed(simpleName: String): Boolean =
            element?.annotationMirrors?.any { it.annotationType.asElement().simpleName.contentEquals(simpleName) } == true

        override fun <A : Annotation> resolveType(annotationType: Class<A>, member: A.() -> KClass<*>): ClassDefinition? {
            val annotation = find(annotationType) ?: return null
            return try {
                elements.getTypeElement(annotation.member().java.name)?.asType()?.let { resolve(it) }
            } catch (mirrored: MirroredTypeException) {
                resolve(mirrored.typeMirror)
            }
        }

        override fun <A : Annotation> resolveTypes(annotationType: Class<A>, member: A.() -> Array<out KClass<*>>): List<ClassDefinition> {
            val annotation = find(annotationType) ?: return emptyList()
            return try {
                annotation.member().mapNotNull { kClass -> elements.getTypeElement(kClass.java.name)?.asType()?.let { resolve(it) } }
            } catch (mirrored: MirroredTypesException) {
                mirrored.typeMirrors.map { resolve(it) }
            }
        }

        override fun memberValues(annotationType: Class<out Annotation>): Map<String, Any?>? {
            val mirror = element?.annotationMirrors?.firstOrNull {
                (it.annotationType.asElement() as? TypeElement)?.qualifiedName?.contentEquals(annotationType.name) == true
            } ?: return null

            val visitor = object : SimpleAnnotationValueVisitor8<Any?, Nothing?>() {
                override fun defaultAction(value: Any?, p: Nothing?): Any? = value
                override fun visitType(t: TypeMirror, p: Nothing?): Any = resolve(t)
                override fun visitEnumConstant(constant: VariableElement, p: Nothing?): Any = constant.simpleName.toString()
                override fun visitArray(values: MutableList<out AnnotationValue>, p: Nothing?): Any = values.map { it.accept(this, null) }
                override fun visitAnnotation(nested: AnnotationMirror, p: Nothing?): Any =
                    elements.getElementValuesWithDefaults(nested).entries
                        .associate { (member, value) -> member.simpleName.toString() to value.accept(this, null) }
            }

            return elements.getElementValuesWithDefaults(mirror).entries
                .associate { (executable, value) -> executable.simpleName.toString() to value.accept(visitor, null) }
        }
    }
}
