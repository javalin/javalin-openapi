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
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types
import io.javalin.introspection.ClassDefinition as RawType
import io.javalin.introspection.StructureType as RawStructureType

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

    private val japIntrospector: JapTypeIntrospector by lazy { JapTypeIntrospector(types, env.elementUtils) { roundEnv } }

    override fun isEnum(type: OpenApiType): Boolean =
        type.source.kind == ElementKind.ENUM

    override fun annotationsOf(type: OpenApiType): Annotations =
        japIntrospector.annotationsOf(type.source)

    fun annotationsOf(element: Element): Annotations =
        japIntrospector.annotationsOf(element)

    override fun propertiesOf(type: OpenApiType): List<PropertyView> =
        japIntrospector.introspect(type.mirror).getProperties()

    override fun enumConstantsOf(type: OpenApiType): List<EnumConstantView> =
        japIntrospector.introspect(type.mirror).getEnumConstants() ?: emptyList()

    @OptIn(InternalIntrospectionApi::class)
    override fun toOpenApiType(raw: RawType): OpenApiType {
        val mirror = raw.source as TypeMirror
        return OpenApiType(
            simpleName = mirror.getSimpleName(),
            fullName = mirror.getFullName(),
            generics = raw.generics.map { toOpenApiType(it) },
            structureType = StructureType.valueOf(raw.structureType.name),
            handle = OpenApiTypeHandle(
                mirror = mirror,
                source = if (raw.structureType == RawStructureType.DICTIONARY) mapType() else (types.asElement(mirror) ?: objectType())
            )
        )
    }

    fun TypeMirror.toOpenApiType(): OpenApiType =
        toOpenApiType(japIntrospector.introspect(this))

    @OptIn(InternalIntrospectionApi::class)
    override fun acceptsProperty(type: OpenApiType, property: PropertyView): Boolean =
        configuration.propertyInSchemeFilter?.filter(this, type, property.source as Element) != false

    override fun discriminatorSubtypes(type: OpenApiType): List<Pair<String, OpenApiType>> {
        val subtypes = japIntrospector.typesAnnotatedWith(DiscriminatorMappingName::class.java, assignableTo = japIntrospector.introspect(type.mirror))
        return subtypes.mapNotNull { subtype ->
            val name = subtype.getAnnotations().memberValues(DiscriminatorMappingName::class.java)?.get("value") as? String
            if (name == null) null else name to toOpenApiType(subtype)
        }
    }

    fun inDebug(body: (Messager) -> Unit) {
        if (configuration.debug) {
            body(env.messager)
        }
    }

    fun forTypeElement(name: String): TypeElement? =
        env.elementUtils.getTypeElement(name)

    private fun objectType(): TypeElement =
        forTypeElement(Object::class.java.name)!!

    private fun mapType(): TypeElement =
        forTypeElement(Map::class.java.name)!!

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

    fun TypeMirror.getSimpleName(): String =
        getFullName().substringAfterLast(".")

    @JvmName("getFullNameExt")
    fun TypeMirror.getFullName(): String =
        getFullName(this)

}
