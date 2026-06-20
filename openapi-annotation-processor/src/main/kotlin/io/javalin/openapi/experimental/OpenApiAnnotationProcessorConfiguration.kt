package io.javalin.openapi.experimental

import io.javalin.openapi.experimental.defaults.ArrayEmbeddedTypeProcessor
import io.javalin.openapi.experimental.defaults.CompositionEmbeddedTypeProcessor
import io.javalin.openapi.experimental.defaults.DictionaryEmbeddedTypeProcessor
import io.javalin.openapi.experimental.defaults.createDefaultSimpleTypeMappings
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
