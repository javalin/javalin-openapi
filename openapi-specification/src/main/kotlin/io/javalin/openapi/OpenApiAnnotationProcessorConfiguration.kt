package io.javalin.openapi

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ExperimentalCompileOpenApiConfiguration

@ExperimentalCompileOpenApiConfiguration
interface OpenApiAnnotationProcessorConfigurer {

    fun configure(configuration: OpenApiAnnotationProcessorConfiguration)

}

@ExperimentalCompileOpenApiConfiguration
class OpenApiAnnotationProcessorConfiguration {
    var debug: Boolean = false
}
