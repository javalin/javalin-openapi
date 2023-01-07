package io.javalin.openapi.experimental.processor.generators

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
import io.javalin.openapi.experimental.AnnotationProcessorContext
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.CustomProperty
import io.javalin.openapi.experimental.processor.shared.createJsonObjectOf
import io.javalin.openapi.experimental.processor.shared.toJsonArray
import io.javalin.openapi.experimental.processor.shared.toJsonObject
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

data class PropertyComposition(
    val type: Composition,
    val references: Set<ClassDefinition>,
    val discriminator: Discriminator
)

fun findCompositionInElement(context: AnnotationProcessorContext, element: Element): PropertyComposition? =
    with (context) {
        element.getAnnotation(OneOf::class.java)?.let { PropertyComposition(ONE_OF, it.getClassDefinitions { value }, it.discriminator) }
            ?: element.getAnnotation(AnyOf::class.java)?.let { PropertyComposition(ANY_OF, it.getClassDefinitions { value }, it.discriminator) }
            ?: element.getAnnotation(AllOf::class.java)?.let { PropertyComposition(ALL_OF, it.getClassDefinitions { value }, it.discriminator) }
    }

fun JsonObject.createComposition(
    context: AnnotationProcessorContext,
    classDefinition: ClassDefinition,
    propertyComposition: PropertyComposition,
    references: MutableSet<ClassDefinition>,
    inlineRefs: Boolean = false,
    requiresNonNulls: Boolean = true,
) {
    with (context) {
        val subtypes by lazy {
            context.roundEnv!!.getElementsAnnotatedWith(DiscriminatorMappingName::class.java)
                .asSequence()
                .filterIsInstance<TypeElement>()
                .map { it.getAnnotation(DiscriminatorMappingName::class.java).value to context.getClassDefinition(it.asType()) }
                .filter { (_, type) -> context.isAssignable(type.mirror, classDefinition.mirror) }
                .toList()
        }

        val refs = propertyComposition.references.ifEmpty { subtypes.map { it.second } }

        when (inlineRefs) {
            true ->
                refs
                    .map { context.typeSchemaGenerator.createTypeSchema(type = it, inlineRefs = true, requireNonNullsByDefault = requiresNonNulls) }
                    .onEach { (_, refs) -> references.addAll(refs) }
                    .map { (scheme, _) -> scheme }
                    .toJsonArray { add(it) }
                    .let { add(propertyComposition.type.propertyName, it) }

            false ->
                refs
                    .onEach { references.add(it) }
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

                val mapping = discriminator.mapping
                    .map { it.name to it.getClassDefinition { value } }
                    .ifEmpty { subtypes }

                if (discriminatorProperty.injectInMappings) {
                    val customProperty = CustomProperty(
                        name = discriminatorProperty.name,
                        type = discriminatorProperty.getClassDefinition { type }
                    )

                    mapping.forEach { (_, mappedClass) ->
                        mappedClass.extra.add(customProperty)
                    }
                }

                mapping
                    .onEach { (_, mappedClass) -> references.add(mappedClass) }
                    .associate { (name, mappedClass) -> name to "#/components/schemas/${mappedClass.simpleName}" }
                    .takeIf { it.isNotEmpty() }
                    ?.also { discriminatorObject.add("mapping", it.toJsonObject()) }
            }
    }
}
