package io.javalin.openapi.experimental

import io.javalin.openapi.experimental.defaults.createDefaultEmbeddedTypeProcessors
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
    val embeddedTypeProcessors: MutableList<EmbeddedTypeProcessor> = createDefaultEmbeddedTypeProcessors()

    fun insertEmbeddedTypeProcessor(embeddedTypeProcessor: EmbeddedTypeProcessor) {
        embeddedTypeProcessors.add(0, embeddedTypeProcessor)
    }
}

fun interface PropertyInSchemeFilter {
    fun filter(context: AnnotationProcessorContext, type: OpenApiType, property: Element): Boolean
}
