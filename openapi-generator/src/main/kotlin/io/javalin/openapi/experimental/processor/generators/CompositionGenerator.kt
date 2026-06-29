package io.javalin.openapi.experimental.processor.generators

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.introspection.AnnotationSet
import io.javalin.openapi.AllOf
import io.javalin.openapi.AnyOf
import io.javalin.openapi.Composition
import io.javalin.openapi.Composition.ALL_OF
import io.javalin.openapi.Composition.ANY_OF
import io.javalin.openapi.Composition.ONE_OF
import io.javalin.openapi.NULL_STRING
import io.javalin.openapi.OneOf
import io.javalin.openapi.experimental.OpenApiType
import io.javalin.openapi.experimental.CustomProperty
import io.javalin.openapi.experimental.SchemaGenerationContext
import io.javalin.openapi.experimental.processor.shared.createJsonObjectOf
import io.javalin.openapi.experimental.processor.shared.createObjectNode
import io.javalin.openapi.experimental.processor.shared.toJsonArray
import io.javalin.openapi.experimental.processor.shared.toJsonObject
import io.javalin.introspection.ClassDefinition as RawType

fun findCompositionInElement(context: SchemaGenerationContext, annotations: AnnotationSet): PropertyComposition? =
    compositionOf(context, annotations, OneOf::class.java, ONE_OF)
        ?: compositionOf(context, annotations, AnyOf::class.java, ANY_OF)
        ?: compositionOf(context, annotations, AllOf::class.java, ALL_OF)

private fun compositionOf(context: SchemaGenerationContext, annotations: AnnotationSet, annotationType: Class<out Annotation>, composition: Composition): PropertyComposition? {
    val annotation = annotations.find(annotationType) ?: return null
    val references = annotation.classValues("value").map { context.toOpenApiType(it) }.toSet()
    val discriminator = (annotation.value("discriminator") as? Map<*, *>)?.let { discriminatorInfo(context, it) }
    return PropertyComposition(composition, references, discriminator)
}

private fun discriminatorInfo(context: SchemaGenerationContext, discriminator: Map<*, *>): DiscriminatorInfo {
    val property = discriminator["property"] as Map<*, *>
    val mapping = (discriminator["mapping"] as? List<*>).orEmpty()
        .filterIsInstance<Map<*, *>>()
        .map { (it["name"] as String) to context.toOpenApiType(it["value"] as RawType) }
    return DiscriminatorInfo(
        propertyName = property["name"] as String,
        propertyType = context.toOpenApiType(property["type"] as RawType),
        injectInMappings = property["injectInMappings"] as Boolean,
        mapping = mapping,
    )
}

fun ObjectNode.createComposition(
    context: SchemaGenerationContext,
    type: OpenApiType,
    propertyComposition: PropertyComposition,
    references: MutableSet<OpenApiType>,
    inlineRefs: Boolean = false,
    requiresNonNulls: Boolean = true,
) {
    val subtypes by lazy { context.discriminatorSubtypes(type) }

    val refs = propertyComposition.references.ifEmpty { subtypes.map { it.second } }

    // an empty oneOf/anyOf/allOf is invalid OpenAPI, so skip when there are no refs/subtypes
    if (refs.isEmpty()) return

    when (inlineRefs) {
        true ->
            refs
                .map { context.typeSchemaGenerator.createTypeSchema(type = it, inlineRefs = true, requireNonNullsByDefault = requiresNonNulls) }
                .onEach { (_, refs) -> references.addAll(refs) }
                .map { (scheme, _) -> scheme }
                .toJsonArray { add(it) }
                .let { set<JsonNode>(propertyComposition.type.propertyName, it) }

        false ->
            refs
                .onEach { references.add(it) }
                .map { createJsonObjectOf($$"$ref", "#/components/schemas/${it.simpleName}") }
                .toJsonArray { add(it) }
                .let { set(propertyComposition.type.propertyName, it) }
    }

    propertyComposition.discriminator
        ?.takeIf { it.propertyName != NULL_STRING }
        ?.also { discriminator ->
            val discriminatorObject = createObjectNode()
            set<JsonNode>("discriminator", discriminatorObject)
            discriminatorObject.put("propertyName", discriminator.propertyName)

            val mapping = discriminator.mapping.ifEmpty { subtypes }

            if (discriminator.injectInMappings) {
                val customProperty = CustomProperty(
                    name = discriminator.propertyName,
                    type = discriminator.propertyType
                )

                mapping.forEach { (_, mappedClass) ->
                    mappedClass.extra.add(customProperty)
                }
            }

            mapping
                .onEach { (_, mappedClass) -> references.add(mappedClass) }
                .associate { (name, mappedClass) -> name to "#/components/schemas/${mappedClass.simpleName}" }
                .takeIf { it.isNotEmpty() }
                ?.also { discriminatorObject.set<JsonNode>("mapping", it.toJsonObject()) }
        }
}
