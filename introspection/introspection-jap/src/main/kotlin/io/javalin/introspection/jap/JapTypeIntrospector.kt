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
import io.javalin.introspection.isSetterName
import io.javalin.introspection.propertyName
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
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable
import javax.lang.model.type.WildcardType
import javax.lang.model.util.Elements
import javax.lang.model.util.SimpleAnnotationValueVisitor8
import javax.lang.model.util.Types

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
            AnnotationsView(listOfNotNull(typeElement()))

        override fun getProperties(): List<PropertyView> {
            val element = typeElement() ?: return emptyList()

            if (element.kind == ElementKind.RECORD) {
                return element.recordComponents.map { component ->
                    PropertyView(
                        name = component.simpleName.toString(),
                        type = resolve(component.asType()),
                        accessors = setOf(Accessor.GETTER, Accessor.FIELD),
                        nullable = component.asType().nullable(),
                        visibility = Visibility.PUBLIC,
                        transient = false,
                        annotations = AnnotationsView(listOf(component)),
                    )
                }
            }

            val builders = LinkedHashMap<String, Acc>()
            for (member in elements.getAllMembers(element)) {
                when {
                    member.isGetter() -> (member as ExecutableElement).let { getter ->
                        builders.getOrPut(propertyName(getter.simpleName.toString())) { Acc() }.apply {
                            accessors += Accessor.GETTER
                            getterType = getter.returnType
                            getterVisibility = getter.visibility()
                            sources += getter
                        }
                    }
                    member.isSetter() -> (member as ExecutableElement).let { setter ->
                        builders.getOrPut(propertyName(setter.simpleName.toString())) { Acc() }.apply {
                            accessors += Accessor.SETTER
                            setterType = setter.parameters[0].asType()
                            sources += setter
                        }
                    }
                    member.isInstanceField() -> (member as VariableElement).let { field ->
                        builders.getOrPut(field.simpleName.toString()) { Acc() }.apply {
                            accessors += Accessor.FIELD
                            fieldType = field.asType()
                            fieldVisibility = field.visibility()
                            transient = Modifier.TRANSIENT in field.modifiers
                            sources += field
                        }
                    }
                }
            }

            return builders.map { (name, acc) ->
                val type = acc.getterType ?: acc.fieldType ?: acc.setterType!!
                PropertyView(
                    name = name,
                    type = resolve(type),
                    accessors = acc.accessors,
                    nullable = type.nullable(),
                    visibility = acc.fieldVisibility ?: acc.getterVisibility ?: Visibility.PUBLIC,
                    transient = acc.transient,
                    annotations = AnnotationsView(acc.sources),
                )
            }
        }

        private inner class Acc {
            val accessors = mutableSetOf<Accessor>()
            val sources = mutableListOf<Element>()
            var getterType: TypeMirror? = null
            var fieldType: TypeMirror? = null
            var setterType: TypeMirror? = null
            var getterVisibility: Visibility? = null
            var fieldVisibility: Visibility? = null
            var transient = false
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

        private fun Element.isSetter(): Boolean {
            if (kind != ElementKind.METHOD || this !is ExecutableElement) return false
            if (Modifier.PUBLIC !in modifiers || Modifier.STATIC in modifiers) return false
            if (parameters.size != 1 || enclosingElement?.toString() == "java.lang.Object") return false
            return isSetterName(simpleName.toString())
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

    private inner class AnnotationsView(private val sources: List<Element>) : Annotations {

        override fun hasNamed(simpleName: String): Boolean =
            sources.any { source -> source.annotationMirrors.any { it.annotationType.asElement().simpleName.contentEquals(simpleName) } }

        override fun memberValues(annotationType: Class<out Annotation>): Map<String, Any?>? {
            val mirror = sources.firstNotNullOfOrNull { source ->
                source.annotationMirrors.firstOrNull {
                    (it.annotationType.asElement() as? TypeElement)?.qualifiedName?.contentEquals(annotationType.name) == true
                }
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
