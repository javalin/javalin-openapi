package io.javalin.openapi.experimental

import com.sun.source.util.Trees
import io.javalin.introspection.Annotations
import io.javalin.introspection.EnumConstantView
import io.javalin.introspection.InternalIntrospectionApi
import io.javalin.introspection.PropertyView
import io.javalin.introspection.jap.JapTypeIntrospector
import io.javalin.openapi.DiscriminatorMappingName
import io.javalin.openapi.OpenApiName
import io.javalin.openapi.experimental.processor.generators.TypeSchemaGenerator
import io.javalin.openapi.experimental.processor.shared.getTypeMirror
import io.javalin.openapi.experimental.processor.shared.getTypeMirrors
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types
import kotlin.reflect.KClass
import io.javalin.introspection.ClassDefinition as RawType

class AnnotationProcessorContext(
    val parameters: OpenApiAnnotationProcessorParameters,
    val configuration: OpenApiAnnotationProcessorConfiguration,
    val env: ProcessingEnvironment,
    val trees: Trees?,
) : SchemaGenerationContext {

    val types: Types = env.typeUtils
    override val typeSchemaGenerator: TypeSchemaGenerator = TypeSchemaGenerator(this)
    var roundEnv: RoundEnvironment? = null

    override val simpleTypeMappings: Map<String, SimpleType> get() = configuration.simpleTypeMappings
    override val embeddedTypeProcessors: List<EmbeddedTypeProcessor> get() = configuration.embeddedTypeProcessors

    private val japIntrospector: JapTypeIntrospector by lazy { JapTypeIntrospector(types, env.elementUtils) }

    fun annotationsOf(element: Element): Annotations =
        japIntrospector.annotationsOf(element)

    fun propertiesOf(mirror: TypeMirror): List<PropertyView> =
        japIntrospector.introspect(mirror).getProperties()

    fun enumConstantsOf(mirror: TypeMirror): List<EnumConstantView> =
        japIntrospector.introspect(mirror).getEnumConstants() ?: emptyList()

    override fun isEnum(type: ClassDefinition): Boolean =
        type.source.kind == ElementKind.ENUM

    override fun annotationsOf(type: ClassDefinition): Annotations =
        annotationsOf(type.source)

    override fun propertiesOf(type: ClassDefinition): List<PropertyView> =
        propertiesOf(type.mirror)

    override fun enumConstantsOf(type: ClassDefinition): List<EnumConstantView> =
        enumConstantsOf(type.mirror)

    override fun toClassDefinition(raw: RawType): ClassDefinition =
        toExperimental(raw)

    @OptIn(InternalIntrospectionApi::class)
    override fun acceptsProperty(type: ClassDefinition, property: PropertyView): Boolean =
        configuration.propertyInSchemeFilter?.filter(this, type, property.source as Element) != false

    override fun discriminatorSubtypes(type: ClassDefinition): List<Pair<String, ClassDefinition>> =
        roundEnv!!.getElementsAnnotatedWith(DiscriminatorMappingName::class.java)
            .asSequence()
            .filterIsInstance<TypeElement>()
            .map { it.getAnnotation(DiscriminatorMappingName::class.java).value to getClassDefinition(it.asType()) }
            .filter { (_, subtype) -> isAssignable(subtype.mirror, type.mirror) }
            .toList()

    fun <R> inContext(body: AnnotationProcessorContext.() -> R): R =
        body()

    fun inDebug(body: (Messager) -> Unit) {
        if (configuration.debug) {
            body(env.messager)
        }
    }

    fun getClassDefinition(mirror: TypeMirror): ClassDefinition =
        classDefinitionFrom(this, mirror)

    fun getClassDefinitions(mirrors: Set<TypeMirror>): Set<ClassDefinition> =
        mirrors.map { getClassDefinition(it) }.toSet()

    fun forTypeElement(name: String): TypeElement? =
        env.elementUtils.getTypeElement(name)

    fun forTypeElement(mirror: TypeMirror): TypeElement =
        env.typeUtils.asElement(mirror) as TypeElement

    fun isAssignable(implementation: TypeMirror, superclass: TypeMirror): Boolean =
        env.typeUtils.isAssignable(implementation, superclass)

    fun hasElement(type: TypeElement, element: Element): Boolean =
        when (element) {
            is ExecutableElement -> env.elementUtils.getAllMembers(type).let { members ->
                members.contains(element) || members.filterIsInstance<ExecutableElement>().any { env.elementUtils.overrides(element, it, type) }
            }
            else -> false
        }

    fun getFullName(mirror: TypeMirror): String =
        env.typeUtils.asElement(mirror)
            ?.getAnnotation(OpenApiName::class.java)
            ?.value
            ?.let { mirror.toString().substringBeforeLast(".") + "." + it }
            ?: env.typeUtils.asElement(mirror)?.toString()?.substringBefore("<")
            ?: mirror.toString().substringBefore("<")

    /* Extension methods, should be replaced by context receivers in the future */

    fun TypeMirror.toClassDefinition(): ClassDefinition =
        getClassDefinition(this)

    fun TypeMirror.getSimpleName(): String =
        getFullName().substringAfterLast(".")

    @JvmName("getFullNameExt")
    fun TypeMirror.getFullName(): String =
        getFullName(this)

    fun <A : Annotation> A.getClassDefinitions(supplier: A.() -> Array<out KClass<*>>): Set<ClassDefinition> =
        getTypeMirrors(supplier)
            .map { it.toClassDefinition() }
            .toSet()

    fun <A : Annotation> A.getClassDefinition(supplier: A.() -> KClass<*>): ClassDefinition =
        getTypeMirror(supplier).toClassDefinition()

}
