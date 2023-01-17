import io.javalin.openapi.experimental.ExperimentalCompileOpenApiConfiguration
import io.javalin.openapi.experimental.OpenApiAnnotationProcessorConfiguration
import io.javalin.openapi.experimental.OpenApiAnnotationProcessorConfigurer

@ExperimentalCompileOpenApiConfiguration
class OpenApiConfiguration implements OpenApiAnnotationProcessorConfigurer {

    @Override
    void configure(OpenApiAnnotationProcessorConfiguration openApiAnnotationProcessorConfiguration) {
        // openApiAnnotationProcessorConfiguration.debug = true
    }

}