import groovy.transform.CompileStatic
import io.javalin.openapi.experimental.ExperimentalCompileOpenApiConfiguration
import io.javalin.openapi.experimental.OpenApiAnnotationProcessorConfiguration
import io.javalin.openapi.experimental.OpenApiAnnotationProcessorConfigurer

@CompileStatic
@ExperimentalCompileOpenApiConfiguration
class OpenApiConfiguration implements OpenApiAnnotationProcessorConfigurer {

    @Override
    void configure(OpenApiAnnotationProcessorConfiguration openApiAnnotationProcessorConfiguration) {
        // openApiAnnotationProcessorConfiguration.debug = true
    }

}