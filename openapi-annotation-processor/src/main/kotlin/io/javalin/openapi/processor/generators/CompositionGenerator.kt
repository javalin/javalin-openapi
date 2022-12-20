package io.javalin.openapi.processor.generators

import com.google.gson.JsonObject
import io.javalin.openapi.AllOf
import io.javalin.openapi.AnyOf
import io.javalin.openapi.Composition
import io.javalin.openapi.Composition.ALL_OF
import io.javalin.openapi.Composition.ANY_OF
import io.javalin.openapi.Composition.ONE_OF
import io.javalin.openapi.Discriminator
import io.javalin.openapi.DiscriminatorMappingName
import io.javalin.openapi.NULL_STRING
import io.javalin.openapi.OneOf
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.processor.OpenApiAnnotationProcessor.Companion.context
import io.javalin.openapi.processor.shared.JsonTypes.getTypeMirror
import io.javalin.openapi.processor.shared.JsonTypes.getTypeMirrors
import io.javalin.openapi.processor.shared.JsonTypes.toClassDefinition
import io.javalin.openapi.processor.shared.createJsonObjectOf
import io.javalin.openapi.processor.shared.getSimpleName
import io.javalin.openapi.processor.shared.toJsonArray
import io.javalin.openapi.processor.shared.toJsonObject
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

data class PropertyComposition(
    val type: Composition,
    val references: List<TypeMirror>,
    val discriminator: Discriminator
)

fun Element.getComposition(): PropertyComposition? =
    getAnnotation(OneOf::class.java)?.let { PropertyComposition(ONE_OF, it.getTypeMirrors { value }, it.discriminator) }
        ?: getAnnotation(AnyOf::class.java)?.let { PropertyComposition(ANY_OF, it.getTypeMirrors { value }, it.discriminator) }
        ?: getAnnotation(AllOf::class.java)?.let { PropertyComposition(ALL_OF, it.getTypeMirrors { value }, it.discriminator) }

fun JsonObject.createComposition(
    classDefinition: ClassDefinition,
    propertyComposition: PropertyComposition,
    references: MutableList<TypeMirror>,
    inlineRefs: Boolean = false,
    requiresNonNulls: Boolean = true,
) {
    val subtypes by lazy {
        context.roundEnv.getElementsAnnotatedWith(DiscriminatorMappingName::class.java)
            .asSequence()
            .filterIsInstance<TypeElement>()
            .map { it.getAnnotation(DiscriminatorMappingName::class.java).value to it.asType() }
            .filter { (_, type) -> context.isAssignable(type, classDefinition.mirror) }
            .toList()
    }

    val refs = propertyComposition.references.ifEmpty { subtypes.map { it.second } }

    when (inlineRefs) {
        true ->
            refs
                .map { createTypeSchema(type = it.toClassDefinition(), inlineRefs = true, requireNonNullsByDefault = requiresNonNulls) }
                .onEach { (_, refs) -> references.addAll(refs) }
                .map { (scheme, _) -> scheme }
                .toJsonArray { add(it) }
                .let { add(propertyComposition.type.propertyName, it) }
        false ->
            refs
                .map { createJsonObjectOf("\$ref", "#/components/schemas/${it.toString().substringAfterLast(".")}") }
                .toJsonArray { add(it) }
                .let { add(propertyComposition.type.propertyName, it) }
    }

    propertyComposition.discriminator
        .takeIf { it.propertyName != NULL_STRING }
        ?.also { discriminator ->
            val discriminatorObject = JsonObject()
            add("discriminator", discriminatorObject)

            discriminatorObject.addProperty("propertyName", discriminator.propertyName)

            discriminator.mappings
                .map { it.name to it.getTypeMirror { value } }
                .ifEmpty { subtypes }
                .onEach { (_, mappedClass) -> references.add(mappedClass)}
                .associate { (name, mappedClass) -> name to "#/components/schemas/${mappedClass.getSimpleName()}"}
                .takeIf { it.isNotEmpty() }
                ?.also { discriminatorObject.add("mappings", it.toJsonObject()) }
        }
}
