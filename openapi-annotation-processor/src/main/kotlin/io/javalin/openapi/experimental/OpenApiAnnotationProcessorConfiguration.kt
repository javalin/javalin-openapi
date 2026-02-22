package io.javalin.openapi.experimental

import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.openapi.experimental.defaults.ArrayEmbeddedTypeProcessor
import io.javalin.openapi.experimental.defaults.CompositionEmbeddedTypeProcessor
import io.javalin.openapi.experimental.defaults.DictionaryEmbeddedTypeProcessor
import io.javalin.openapi.experimental.defaults.createDefaultSimpleTypeMappings
import io.javalin.openapi.experimental.processor.generators.PropertyComposition
import javax.lang.model.element.Element

@ExperimentalCompileOpenApiConfiguration
interface OpenApiAnnotationProcessorConfigurer {
    fun configure(configuration: OpenApiAnnotationProcessorConfiguration)
}

class OpenApiAnnotationProcessorConfiguration {
    var debug: Boolean = false
    var validateWithParser: Boolean = true
    var propertyInSchemeFilter: PropertyInSchemeFilter? = null
    val simpleTypeMappings: MutableMap<String, SimpleType> = createDefaultSimpleTypeMappings()
    val embeddedTypeProcessors: MutableList<EmbeddedTypeProcessor> = mutableListOf(
        CompositionEmbeddedTypeProcessor(),
        ArrayEmbeddedTypeProcessor(),
        DictionaryEmbeddedTypeProcessor()
    )

    fun insertEmbeddedTypeProcessor(embeddedTypeProcessor: EmbeddedTypeProcessor) {
        embeddedTypeProcessors.add(0, embeddedTypeProcessor)
    }

}

fun interface PropertyInSchemeFilter {
    fun filter(context: AnnotationProcessorContext, type: ClassDefinition, property: Element): Boolean
}

data class EmbeddedTypeProcessorContext(
    val parentContext: AnnotationProcessorContext,
    val scheme: ObjectNode,
    val references: MutableSet<ClassDefinition>,
    val type: ClassDefinition,
    val inlineRefs: Boolean = false,
    val requiresNonNulls: Boolean = true,
    val composition: PropertyComposition? = null,
    val extra: Map<String, Any?> = emptyMap()
)

fun interface EmbeddedTypeProcessor {
    fun process(context: EmbeddedTypeProcessorContext): Boolean
}
