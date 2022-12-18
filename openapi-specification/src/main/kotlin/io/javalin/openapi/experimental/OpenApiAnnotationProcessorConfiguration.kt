package io.javalin.openapi.experimental

import javax.lang.model.element.Element

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ExperimentalCompileOpenApiConfiguration

@ExperimentalCompileOpenApiConfiguration
interface OpenApiAnnotationProcessorConfigurer {
    fun configure(configuration: OpenApiAnnotationProcessorConfiguration)
}

class OpenApiAnnotationProcessorConfiguration {
    var debug: Boolean = false
    var propertyInSchemeFilter: PropertyInSchemeFilter? = null
}

fun interface PropertyInSchemeFilter {
    fun filter(context: AnnotationProcessorContext, type: ClassDefinition, property: Element): Boolean
}
