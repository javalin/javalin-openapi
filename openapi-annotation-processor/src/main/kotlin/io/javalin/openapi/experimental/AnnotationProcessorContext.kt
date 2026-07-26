package io.javalin.openapi.experimental

import com.sun.source.util.Trees
import io.javalin.introspection.AnnotationSet
import io.javalin.introspection.EnumConstant
import io.javalin.introspection.InternalIntrospectionApi
import io.javalin.introspection.PropertyProjection
import io.javalin.introspection.ClassDefinition as RawType
import io.javalin.introspection.StructureType as RawStructureType
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
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types
import javax.tools.Diagnostic.Kind.NOTE
import javax.tools.Diagnostic.Kind.WARNING

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

    private val japIntrospector: JapTypeIntrospector by lazy {
        JapTypeIntrospector(
            types = types,
            elements = env.elementUtils,
        ) { roundEnv }
    }

    override fun isEnum(type: OpenApiType): Boolean =
        type.source.kind == ElementKind.ENUM

    override fun annotationsOf(type: OpenApiType): AnnotationSet =
        japIntrospector.annotationsOf(type.source)

    fun annotationsOf(element: Element): AnnotationSet =
        japIntrospector.annotationsOf(element)

    override fun propertiesOf(type: OpenApiType): List<PropertyProjection> =
        japIntrospector.introspect(type.mirror).getProperties()

    override fun enumConstantsOf(type: OpenApiType): List<EnumConstant> =
        japIntrospector.introspect(type.mirror).getEnumConstants()

    @OptIn(InternalIntrospectionApi::class)
    override fun toOpenApiType(raw: RawType): OpenApiType {
        val rawMirror = raw.source as TypeMirror
        val mirror = when {
            rawMirror.kind.isPrimitive -> types.boxedClass(rawMirror as PrimitiveType).asType()
            else -> rawMirror
        }
        val source = when {
            raw.structureType == RawStructureType.DICTIONARY -> mapType()
            else -> types.asElement(mirror) ?: objectType()
        }

        return OpenApiType(
            simpleName = mirror.getSimpleName(),
            fullName = mirror.getFullName(),
            generics = raw.generics.map { toOpenApiType(it) },
            structureType = StructureType.valueOf(raw.structureType.name),
            handle = OpenApiTypeHandle(
                mirror = mirror,
                source = source,
            ),
        )
    }

    override fun reportWarning(message: String) {
        env.messager.printMessage(WARNING, message)
    }

    override fun reportDebug(message: String) {
        inDebug { it.printMessage(NOTE, message) }
    }

    fun TypeMirror.toOpenApiType(): OpenApiType =
        toOpenApiType(japIntrospector.introspect(this))

    @OptIn(InternalIntrospectionApi::class)
    override fun acceptsProperty(type: OpenApiType, property: PropertyProjection): Boolean =
        configuration.propertyInSchemeFilter?.filter(this, type, property.source as Element) != false

    override fun discriminatorSubtypes(type: OpenApiType): List<Pair<String, OpenApiType>> {
        val source = japIntrospector.introspect(type.mirror)
        val subtypes = japIntrospector.typesAnnotatedWith(
            annotationType = DiscriminatorMappingName::class.java,
            assignableTo = source,
        )
        return subtypes.mapNotNull { subtype ->
            subtype
                .getAnnotations()
                .find(DiscriminatorMappingName::class.java)
                ?.get("value")
                ?.asString()
                ?.let { name -> name to toOpenApiType(subtype) }
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
                members.contains(element) ||
                    members
                        .filterIsInstance<ExecutableElement>()
                        .any { env.elementUtils.overrides(element, it, type) }
            }
            else -> false
        }

    fun getFullName(mirror: TypeMirror): String {
        val element = env.typeUtils.asElement(mirror)
        val customName = element?.getAnnotation(OpenApiName::class.java)?.value
        return when {
            customName != null -> mirror.toString().substringBeforeLast(".") + "." + customName
            else -> element?.toString()?.substringBefore("<") ?: mirror.toString().substringBefore("<")
        }
    }

    fun TypeMirror.getSimpleName(): String =
        getFullName().substringAfterLast(".")

    @JvmName("getFullNameExt")
    fun TypeMirror.getFullName(): String =
        getFullName(this)

}
