package io.javalin.openapi.experimental

import javax.lang.model.element.Element
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

@RequiresOptIn
@Retention(BINARY)
@Target(CLASS, FUNCTION)
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
