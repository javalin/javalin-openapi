package io.javalin.introspection.jap

import io.javalin.introspection.Accessor
import io.javalin.introspection.AnnotationView
import io.javalin.introspection.Annotations
import io.javalin.introspection.ClassDefinition
import io.javalin.introspection.CompileTimeIntrospector
import io.javalin.introspection.EnumConstantView
import io.javalin.introspection.InternalIntrospectionApi
import io.javalin.introspection.PropertyView
import io.javalin.introspection.StructureType
import io.javalin.introspection.StructureType.ARRAY
import io.javalin.introspection.StructureType.DEFAULT
import io.javalin.introspection.StructureType.DICTIONARY
import io.javalin.introspection.Visibility
import io.javalin.introspection.isGetterName
import io.javalin.introspection.propertyName
import javax.annotation.processing.RoundEnvironment
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

class JapTypeIntrospector(
    private val types: Types,
    private val elements: Elements,
    private val roundEnvProvider: () -> RoundEnvironment? = { null },
) : CompileTimeIntrospector {

    override fun introspect(source: Any): ClassDefinition {
        require(source is TypeMirror) { "JapTypeIntrospector expects a javax.lang.model.type.TypeMirror, got ${source::class.java.name}" }
        return resolve(source)
    }

    fun annotationsOf(element: Element): Annotations =
        AnnotationsView(listOf(element))

    @OptIn(InternalIntrospectionApi::class)
    override fun typesAnnotatedWith(annotationType: Class<out Annotation>, assignableTo: ClassDefinition?): List<ClassDefinition> {
        val roundEnv = roundEnvProvider() ?: return emptyList()
        val target = assignableTo?.source as? TypeMirror
        return roundEnv.getElementsAnnotatedWith(annotationType)
            .filterIsInstance<TypeElement>()
            .filter { target == null || types.isAssignable(it.asType(), target) }
            .map { resolve(it.asType()) }
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

    private fun mirrorValues(mirror: AnnotationMirror): Map<String, Any?> {
        val visitor = object : SimpleAnnotationValueVisitor8<Any?, Nothing?>() {
            override fun defaultAction(value: Any?, p: Nothing?): Any? = value
            override fun visitType(t: TypeMirror, p: Nothing?): Any = resolve(t)
            override fun visitEnumConstant(constant: VariableElement, p: Nothing?): Any = constant.simpleName.toString()
            override fun visitArray(values: MutableList<out AnnotationValue>, p: Nothing?): Any = values.map { it.accept(this, null) }
            override fun visitAnnotation(nested: AnnotationMirror, p: Nothing?): Any = mirrorValues(nested)
        }
        return elements.getElementValuesWithDefaults(mirror).entries
            .associate { (member, value) -> member.simpleName.toString() to value.accept(visitor, null) }
    }

    private inner class Definition(
        simpleName: String,
        fullName: String,
        generics: List<ClassDefinition>,
        structureType: StructureType,
        private val mirror: TypeMirror,
    ) : ClassDefinition(simpleName, fullName, generics, structureType) {

        @InternalIntrospectionApi
        override val source: Any
            get() = mirror

        override fun isEnum(): Boolean =
            typeElement()?.kind == ElementKind.ENUM

        override fun getEnumConstants(): List<EnumConstantView>? =
            typeElement()
                ?.takeIf { it.kind == ElementKind.ENUM }
                ?.enclosedElements
                ?.filter { it.kind == ElementKind.ENUM_CONSTANT }
                ?.map { EnumConstantView(it.simpleName.toString(), AnnotationsView(listOf(it))) }

        override fun getAnnotations(): Annotations =
            AnnotationsView(listOfNotNull(typeElement()))

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
                        source = component,
                        annotations = AnnotationsView(listOf(component)),
                    )
                }
            }

            return elements.getAllMembers(element).mapNotNull { member ->
                when {
                    member.isGetter() -> (member as ExecutableElement).let { getter ->
                        PropertyView(
                            name = propertyName(getter.simpleName.toString()),
                            type = resolve(getter.returnType),
                            accessor = Accessor.GETTER,
                            nullable = getter.returnType.nullable(),
                            visibility = getter.visibility(),
                            transient = false,
                            source = getter,
                            annotations = AnnotationsView(listOf(getter)),
                        )
                    }
                    member.isInstanceField() -> (member as VariableElement).let { field ->
                        PropertyView(
                            name = field.simpleName.toString(),
                            type = resolve(field.asType()),
                            accessor = Accessor.FIELD,
                            nullable = field.asType().nullable(),
                            visibility = field.visibility(),
                            transient = Modifier.TRANSIENT in field.modifiers,
                            source = field,
                            annotations = AnnotationsView(listOf(field)),
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
            if (Modifier.STATIC in modifiers) return false
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

    private inner class AnnotationsView(private val sources: List<Element>) : Annotations {

        override fun hasNamed(simpleName: String): Boolean =
            sources.any { source -> source.annotationMirrors.any { it.annotationType.asElement().simpleName.contentEquals(simpleName) } }

        override fun memberValues(annotationType: Class<out Annotation>): Map<String, Any?>? =
            sources.firstNotNullOfOrNull { source -> source.annotationMirrors.firstOrNull { it.named(annotationType.name) } }
                ?.let { mirrorValues(it) }

        override fun memberValuesList(annotationType: Class<out Annotation>): List<Map<String, Any?>> {
            val mirrors = sources.flatMap { it.annotationMirrors }
            val direct = mirrors.filter { it.named(annotationType.name) }.map { mirrorValues(it) }
            // javac wraps repeated annotations in their @Repeatable container; unwrap its `value` array
            val containerName = annotationType.getAnnotation(java.lang.annotation.Repeatable::class.java)?.value?.java?.canonicalName
            val repeated = containerName
                ?.let { name -> mirrors.firstOrNull { it.named(name) } }
                ?.let { mirrorValues(it)["value"] as? List<*> }
                ?.filterIsInstance<Map<String, Any?>>()
                .orEmpty()
            return direct + repeated
        }

        override fun all(): List<AnnotationView> =
            sources.flatMap { it.annotationMirrors }.distinctBy { it.annotationType.asElement() }.map { JapAnnotationView(it) }

        private fun AnnotationMirror.named(qualifiedName: String): Boolean =
            (annotationType.asElement() as? TypeElement)?.qualifiedName?.contentEquals(qualifiedName) == true
    }

    private inner class JapAnnotationView(private val mirror: AnnotationMirror) : AnnotationView {
        override val qualifiedName: String
            get() = (mirror.annotationType.asElement() as? TypeElement)?.qualifiedName?.toString() ?: simpleName
        override val simpleName: String
            get() = mirror.annotationType.asElement().simpleName.toString()
        override val meta: Annotations
            get() = AnnotationsView(listOf(mirror.annotationType.asElement()))
        override fun values(): Map<String, Any?> =
            mirrorValues(mirror)
    }
}
