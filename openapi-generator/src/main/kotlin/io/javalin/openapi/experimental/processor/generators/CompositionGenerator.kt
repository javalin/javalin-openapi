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
import io.javalin.openapi.experimental.CustomProperty
import io.javalin.openapi.experimental.OpenApiType
import io.javalin.openapi.experimental.SchemaGenerationContext
import io.javalin.openapi.experimental.processor.shared.createArrayNode
import io.javalin.openapi.experimental.processor.shared.createJsonObjectOf
import io.javalin.openapi.experimental.processor.shared.createObjectNode
import io.javalin.openapi.experimental.processor.shared.toJsonObject
import io.javalin.introspection.ClassDefinition as RawType

fun findCompositionInElement(context: SchemaGenerationContext, annotations: AnnotationSet): PropertyComposition? =
    compositionOf(context, annotations, OneOf::class.java, ONE_OF)
        ?: compositionOf(context, annotations, AnyOf::class.java, ANY_OF)
        ?: compositionOf(context, annotations, AllOf::class.java, ALL_OF)

private fun compositionOf(
    context: SchemaGenerationContext,
    annotations: AnnotationSet,
    annotationType: Class<out Annotation>,
    composition: Composition,
): PropertyComposition? {
    val annotation = annotations.find(annotationType) ?: return null
    val references = annotation.get("value").asClassDefinitions().map { context.toOpenApiType(it) }.toSet()
    val discriminator = annotation.get("discriminator").asMap()?.let { discriminatorInfo(context, it) }
    return PropertyComposition(
        type = composition,
        references = references,
        discriminator = discriminator,
    )
}

private fun discriminatorInfo(context: SchemaGenerationContext, discriminator: Map<*, *>): DiscriminatorInfo {
    val property = discriminator["property"] as Map<*, *>
    val mapping = (discriminator["mapping"] as? List<*>).orEmpty()
        .filterIsInstance<Map<*, *>>()
        .map { entry ->
            val name = entry["name"] as String
            val type = entry["value"] as RawType
            name to context.toOpenApiType(type)
        }
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

    val compositionValues = createArrayNode()
    if (inlineRefs) {
        for (ref in refs) {
            val result = context.typeSchemaGenerator.createTypeSchema(
                type = ref,
                inlineRefs = true,
                requireNonNullsByDefault = requiresNonNulls,
            )
            references.addAll(result.references)
            compositionValues.add(result.json)
        }
    } else {
        for (ref in refs) {
            references.add(ref)
            compositionValues.add(createJsonObjectOf($$"$ref", "#/components/schemas/${ref.simpleName}"))
        }
    }
    set<JsonNode>(propertyComposition.type.propertyName, compositionValues)

    val discriminator = propertyComposition.discriminator
        ?.takeIf { it.propertyName != NULL_STRING }
        ?: return

    val discriminatorObject = createObjectNode()
    set<JsonNode>("discriminator", discriminatorObject)
    discriminatorObject.put("propertyName", discriminator.propertyName)

    val mapping = discriminator.mapping.ifEmpty { subtypes }
    if (discriminator.injectInMappings) {
        val customProperty = CustomProperty(
            name = discriminator.propertyName,
            type = discriminator.propertyType,
        )

        mapping.forEach { (_, mappedClass) ->
            mappedClass.extra.add(customProperty)
        }
    }

    if (mapping.isNotEmpty()) {
        mapping.forEach { (_, mappedClass) -> references.add(mappedClass) }
        val mappings = mapping.associate { (name, mappedClass) ->
            name to "#/components/schemas/${mappedClass.simpleName}"
        }
        discriminatorObject.set<JsonNode>("mapping", mappings.toJsonObject())
    }
}
