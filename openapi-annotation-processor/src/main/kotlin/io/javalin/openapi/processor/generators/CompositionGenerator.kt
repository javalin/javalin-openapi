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
import io.javalin.openapi.experimental.CustomProperty
import io.javalin.openapi.processor.OpenApiAnnotationProcessor.Companion.context
import io.javalin.openapi.processor.shared.createJsonObjectOf
import io.javalin.openapi.processor.shared.getClassDefinition
import io.javalin.openapi.processor.shared.getClassDefinitions
import io.javalin.openapi.processor.shared.toClassDefinition
import io.javalin.openapi.processor.shared.toJsonArray
import io.javalin.openapi.processor.shared.toJsonObject
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

data class PropertyComposition(
    val type: Composition,
    val references: Set<ClassDefinition>,
    val discriminator: Discriminator
)

fun Element.getComposition(): PropertyComposition? =
    getAnnotation(OneOf::class.java)?.let { PropertyComposition(ONE_OF, it.getClassDefinitions { value }, it.discriminator) }
        ?: getAnnotation(AnyOf::class.java)?.let { PropertyComposition(ANY_OF, it.getClassDefinitions { value }, it.discriminator) }
        ?: getAnnotation(AllOf::class.java)?.let { PropertyComposition(ALL_OF, it.getClassDefinitions { value }, it.discriminator) }

fun JsonObject.createComposition(
    classDefinition: ClassDefinition,
    propertyComposition: PropertyComposition,
    references: MutableSet<ClassDefinition>,
    inlineRefs: Boolean = false,
    requiresNonNulls: Boolean = true,
) {
    val subtypes by lazy {
        context.roundEnv.getElementsAnnotatedWith(DiscriminatorMappingName::class.java)
            .asSequence()
            .filterIsInstance<TypeElement>()
            .map { it.getAnnotation(DiscriminatorMappingName::class.java).value to it.asType().toClassDefinition() }
            .filter { (_, type) -> context.isAssignable(type.mirror, classDefinition.mirror) }
            .toList()
    }

    val refs = propertyComposition.references.ifEmpty { subtypes.map { it.second } }

    when (inlineRefs) {
        true ->
            refs
                .map { createTypeSchema(type = it, inlineRefs = true, requireNonNullsByDefault = requiresNonNulls) }
                .onEach { (_, refs) -> references.addAll(refs) }
                .map { (scheme, _) -> scheme }
                .toJsonArray { add(it) }
                .let { add(propertyComposition.type.propertyName, it) }
        false ->
            refs
                .map { createJsonObjectOf("\$ref", "#/components/schemas/${it.simpleName}") }
                .toJsonArray { add(it) }
                .let { add(propertyComposition.type.propertyName, it) }
    }

    propertyComposition.discriminator
        .takeIf { it.property.name != NULL_STRING }
        ?.also { discriminator ->
            val discriminatorObject = JsonObject()
            add("discriminator", discriminatorObject)

            val discriminatorProperty = discriminator.property
            discriminatorObject.addProperty("propertyName", discriminatorProperty.name)

            val mappings = discriminator.mappings
                .map { it.name to it.getClassDefinition { value } }
                .ifEmpty { subtypes }

            if (discriminatorProperty.injectInMappings) {
                val customProperty = CustomProperty(
                    name = discriminatorProperty.name,
                    type = discriminatorProperty.getClassDefinition { type }
                )

                mappings.forEach { (_, mappedClass) ->
                    mappedClass.extra.add(customProperty)
                }
            }

            mappings
                .onEach { (_, mappedClass) -> references.add(mappedClass)}
                .associate { (name, mappedClass) -> name to "#/components/schemas/${mappedClass.simpleName}"}
                .takeIf { it.isNotEmpty() }
                ?.also { discriminatorObject.add("mappings", it.toJsonObject()) }
        }
}
