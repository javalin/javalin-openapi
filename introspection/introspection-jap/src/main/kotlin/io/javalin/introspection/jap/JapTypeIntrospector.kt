package io.javalin.introspection.jap

import io.javalin.introspection.Accessor
import io.javalin.introspection.AnnotationProjection
import io.javalin.introspection.AnnotationSet
import io.javalin.introspection.ClassDefinition
import io.javalin.introspection.CompileTimeIntrospector
import io.javalin.introspection.EnumConstant
import io.javalin.introspection.InternalIntrospectionApi
import io.javalin.introspection.MemberVisibility
import io.javalin.introspection.PropertyProjection
import io.javalin.introspection.RepeatableAnnotationProjection
import io.javalin.introspection.StructureType
import io.javalin.introspection.StructureType.ARRAY
import io.javalin.introspection.StructureType.DEFAULT
import io.javalin.introspection.StructureType.DICTIONARY
import io.javalin.introspection.isGetterName
import io.javalin.introspection.propertyName
import java.lang.annotation.Inherited
import java.lang.annotation.Repeatable as JavaRepeatable
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

    fun annotationsOf(element: Element): AnnotationSet =
        AnnotationsView(listOf(element), includeInherited = element is TypeElement)

    @OptIn(InternalIntrospectionApi::class)
    override fun typesAnnotatedWith(annotationType: Class<out Annotation>, assignableTo: ClassDefinition?): List<ClassDefinition> {
        val roundEnv = roundEnvProvider() ?: return emptyList()
        val target = assignableTo?.source as? TypeMirror
        return roundEnv.getElementsAnnotatedWith(annotationType)
            .filterIsInstance<TypeElement>()
            .filter { target == null || types.isAssignable(it.asType(), target) }
            .map { resolve(it.asType()) }
    }

    private fun resolve(
        mirror: TypeMirror,
        generics: List<ClassDefinition> = emptyList(),
        structureType: StructureType = DEFAULT,
        visitingTypeVariables: Set<String> = emptySet(),
    ): ClassDefinition =
        when (mirror) {
            is TypeVariable -> {
                val key = mirror.asElement()?.toString() ?: mirror.toString()
                if (key in visitingTypeVariables) {
                    objectDefinition(structureType)
                } else {
                    val bound = mirror.upperBound ?: mirror.lowerBound
                    if (bound != null) {
                        resolve(
                            mirror = bound,
                            generics = generics,
                            structureType = structureType,
                            visitingTypeVariables = visitingTypeVariables + key,
                        )
                    } else {
                        objectDefinition(structureType)
                    }
                }
            }
            is WildcardType ->
                resolve(
                    mirror = mirror.extendsBound ?: objectMirror(),
                    generics = generics,
                    structureType = structureType,
                    visitingTypeVariables = visitingTypeVariables,
                )
            is ArrayType -> resolve(
                mirror = mirror.componentType,
                generics = generics,
                structureType = ARRAY,
                visitingTypeVariables = visitingTypeVariables,
            )
            is PrimitiveType -> definition(
                mirror = types.boxedClass(mirror).asType(),
                generics = generics,
                structureType = structureType,
            )
            is DeclaredType -> declared(
                mirror = mirror,
                generics = generics,
                structureType = structureType,
                visitingTypeVariables = visitingTypeVariables,
            )
            else ->
                types
                    .asElement(mirror)
                    ?.asType()
                    ?.takeIf { it != mirror }
                    ?.let {
                        resolve(
                            mirror = it,
                            generics = generics,
                            structureType = structureType,
                            visitingTypeVariables = visitingTypeVariables
                        )
                    }
                    ?: objectDefinition(structureType)
        }

    private fun declared(
        mirror: DeclaredType,
        generics: List<ClassDefinition>,
        structureType: StructureType,
        visitingTypeVariables: Set<String>,
    ): ClassDefinition {
        val erasure = types.erasure(mirror)
        return when {
            types.isAssignable(erasure, erasureOf("java.util.Map")) -> {
                val keyType = resolve(
                    mirror.typeArguments.getOrElse(0) { objectMirror() },
                    visitingTypeVariables = visitingTypeVariables,
                )
                val valueType = resolve(
                    mirror.typeArguments.getOrElse(1) { objectMirror() },
                    visitingTypeVariables = visitingTypeVariables,
                )
                definition(
                    mirror = mirror,
                    generics = listOf(keyType, valueType),
                    structureType = DICTIONARY,
                )
            }
            types.isAssignable(erasure, erasureOf("java.util.Collection")) ->
                resolve(
                    mirror = mirror.typeArguments.getOrElse(0) { objectMirror() },
                    generics = generics,
                    structureType = ARRAY,
                    visitingTypeVariables = visitingTypeVariables,
                )
            else ->
                definition(
                    mirror = mirror,
                    generics = mirror.typeArguments.map { resolve(it, visitingTypeVariables = visitingTypeVariables) },
                    structureType = structureType,
                )
        }
    }

    private fun definition(
        mirror: TypeMirror,
        generics: List<ClassDefinition>,
        structureType: StructureType,
        sourceMirror: TypeMirror = mirror,
    ): ClassDefinition {
        val element = types.asElement(mirror) as? TypeElement
        val fullName = element?.qualifiedName?.toString() ?: mirror.toString().substringBefore("<")

        return Definition(
            simpleName = element?.simpleName?.toString() ?: fullName.substringAfterLast('.'),
            fullName = fullName,
            generics = generics,
            structureType = structureType,
            mirror = mirror,
            sourceMirror = sourceMirror,
        )
    }

    private fun primitiveDefinition(mirror: PrimitiveType): ClassDefinition =
        definition(
            mirror = types.boxedClass(mirror).asType(),
            generics = emptyList(),
            structureType = DEFAULT,
            sourceMirror = mirror,
        )

    private fun objectDefinition(structureType: StructureType = DEFAULT): ClassDefinition =
        definition(
            mirror = objectMirror(),
            generics = emptyList(),
            structureType = structureType,
        )

    private fun objectMirror(): TypeMirror =
        elements.getTypeElement("java.lang.Object").asType()

    private fun erasureOf(name: String): TypeMirror =
        types.erasure(elements.getTypeElement(name).asType())

    private val annotationMirrorValueVisitor = object : SimpleAnnotationValueVisitor8<Any?, Nothing?>() {
        override fun defaultAction(value: Any?, p: Nothing?): Any? = value

        override fun visitType(type: TypeMirror, p: Nothing?): Any =
            if (type is PrimitiveType) primitiveDefinition(type) else resolve(type)

        override fun visitEnumConstant(constant: VariableElement, p: Nothing?): Any =
            constant.simpleName.toString()

        override fun visitArray(values: MutableList<out AnnotationValue>, p: Nothing?): Any =
            values.map { it.accept(this, null) }

        override fun visitAnnotation(annotation: AnnotationMirror, p: Nothing?): Any =
            mirrorValues(annotation)
    }

    private fun mirrorValues(mirror: AnnotationMirror): Map<String, Any?> =
        elements
            .getElementValuesWithDefaults(mirror)
            .entries
            .associate { (member, value) -> member.simpleName.toString() to value.accept(annotationMirrorValueVisitor, null) }

    private inner class Definition(
        simpleName: String,
        fullName: String,
        generics: List<ClassDefinition>,
        structureType: StructureType,
        private val mirror: TypeMirror,
        private val sourceMirror: TypeMirror = mirror,
    ) : ClassDefinition(
        simpleName = simpleName,
        fullName = fullName,
        generics = generics,
        structureType = structureType,
    ) {

        @InternalIntrospectionApi
        override val source: Any
            get() = sourceMirror

        override fun isEnum(): Boolean =
            typeElement()?.kind == ElementKind.ENUM

        override fun getEnumConstants(): List<EnumConstant> =
            typeElement()
                ?.takeIf { it.kind == ElementKind.ENUM }
                ?.enclosedElements
                ?.filter { it.kind == ElementKind.ENUM_CONSTANT }
                ?.map {
                    EnumConstant(
                        name = it.simpleName.toString(),
                        annotations = AnnotationsView(sources = listOf(it)),
                    )
                }
                .orEmpty()

        override fun getAnnotations(): AnnotationSet =
            AnnotationsView(
                sources = listOfNotNull(typeElement()),
                includeInherited = true,
            )

        override fun getProperties(): List<PropertyProjection> {
            val element = typeElement() ?: return emptyList()

            if (element.kind == ElementKind.RECORD) {
                val recordProperties = element.recordComponents.map { component ->
                    PropertyProjection(
                        name = component.simpleName.toString(),
                        type = resolve(component.asType()),
                        accessor = Accessor.RECORD_COMPONENT,
                        nullable = component.asType().nullable(),
                        visibility = MemberVisibility.PUBLIC,
                        transient = false,
                        source = component,
                        annotations = AnnotationsView(listOf(component)),
                    )
                }
                val recordPropertyNames = recordProperties.mapTo(mutableSetOf()) { it.name }
                val extraGetters = elements.getAllMembers(element).mapNotNull { member ->
                    if (!member.isGetter()) {
                        return@mapNotNull null
                    }

                    val getter = member as ExecutableElement
                    val name = propertyName(getter.simpleName.toString())
                    if (name in recordPropertyNames) {
                        return@mapNotNull null
                    }

                    PropertyProjection(
                        name = name,
                        type = resolve(getter.returnType),
                        accessor = Accessor.GETTER,
                        nullable = getter.returnType.nullable(),
                        visibility = getter.visibility(),
                        transient = false,
                        source = getter,
                        annotations = AnnotationsView(listOf(getter)),
                    )
                }

                return recordProperties + extraGetters
            }

            return elements.getAllMembers(element).mapNotNull { member ->
                when {
                    member.isGetter() -> (member as ExecutableElement).let { getter ->
                        PropertyProjection(
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
                        PropertyProjection(
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

        private fun Element.isGetter(): Boolean =
            when {
                kind != ElementKind.METHOD || this !is ExecutableElement -> false
                Modifier.STATIC in modifiers -> false
                parameters.isNotEmpty() || enclosingElement.toString() == "java.lang.Object" -> false
                returnType.kind == TypeKind.VOID -> false
                else -> isGetterName(simpleName.toString()) || annotationMirrors.any { it.annotationType.asElement().simpleName.contentEquals("OpenApiName") }
            }

        private fun Element.isInstanceField(): Boolean =
            kind == ElementKind.FIELD && this is VariableElement && Modifier.STATIC !in modifiers

        private fun Element.visibility(): MemberVisibility =
            when {
                Modifier.PUBLIC in modifiers -> MemberVisibility.PUBLIC
                Modifier.PROTECTED in modifiers -> MemberVisibility.PROTECTED
                Modifier.PRIVATE in modifiers -> MemberVisibility.PRIVATE
                else -> MemberVisibility.PACKAGE_PRIVATE
            }
    }

    private inner class AnnotationsView(
        private val sources: List<Element>,
        private val includeInherited: Boolean = false,
    ) : AnnotationSet {

        private fun mirrors(source: Element): List<AnnotationMirror> {
            if (!includeInherited) {
                return source.annotationMirrors
            }

            val superclass = (source as? TypeElement)?.superclass
            val inherited =
                generateSequence(superclass) { mirror -> (types.asElement(mirror) as? TypeElement)?.superclass }
                    .mapNotNull { types.asElement(it) as? TypeElement }
                    .takeWhile { it.qualifiedName.toString() != Object::class.java.name }
                    .flatMap { supertype ->
                        supertype
                            .annotationMirrors
                            .filter { it.annotationType.asElement().hasAnnotation(Inherited::class.java) }
                    }
                    .toList()

            return source.annotationMirrors + inherited
        }

        override fun contains(simpleName: String): Boolean =
            sources.any { source ->
                mirrors(source).any {
                    it.annotationType.asElement().simpleName.contentEquals(simpleName)
                }
            }

        override fun find(type: Class<out Annotation>): AnnotationProjection? =
            sources
                .firstNotNullOfOrNull { source -> mirrors(source).firstOrNull { it.named(type) } }
                ?.let { JapAnnotationProjection(it) }

        override fun findAll(type: Class<out Annotation>): List<AnnotationProjection> {
            val annotationMirrors = sources.flatMap { mirrors(it) }
            val direct = annotationMirrors.filter { it.named(type) }.map { JapAnnotationProjection(it) }
            // javac wraps repeated annotations in their @Repeatable container; unwrap its `value` array
            val containerName = type.getAnnotation(JavaRepeatable::class.java)?.value?.java?.canonicalName
            val container = containerName?.let { name -> annotationMirrors.firstOrNull { it.named(name) } }
            val repeated = (container?.let { mirrorValues(it)["value"] } as? List<*>)
                ?.filterIsInstance<Map<String, Any?>>()
                ?.map { RepeatableAnnotationProjection(type.simpleName, it) }
                .orEmpty()
            return direct + repeated
        }

        override fun all(): List<AnnotationProjection> =
            sources
                .flatMap { mirrors(it) }
                .distinctBy { it.annotationType.asElement() }
                .map { JapAnnotationProjection(it) }

        private fun AnnotationMirror.named(qualifiedName: String): Boolean =
            (annotationType.asElement() as? TypeElement)
                ?.qualifiedName
                ?.contentEquals(qualifiedName) == true

        private fun AnnotationMirror.named(type: Class<out Annotation>): Boolean =
            named(type.name) || type.canonicalName?.let { named(it) } == true

        private fun Element.hasAnnotation(type: Class<out Annotation>): Boolean =
            annotationMirrors.any { it.named(type) }

    }

    private inner class JapAnnotationProjection(private val mirror: AnnotationMirror) : AnnotationProjection {

        override val simpleName: String
            get() = mirror.annotationType.asElement().simpleName.toString()

        override val metadata: AnnotationSet
            get() = AnnotationsView(listOf(mirror.annotationType.asElement()))

        override val values: Map<String, Any?>
            get() = mirrorValues(mirror)

    }
}
