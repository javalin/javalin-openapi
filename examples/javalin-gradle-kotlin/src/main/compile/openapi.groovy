import groovy.transform.CompileStatic
import io.javalin.openapi.ExperimentalCompileOpenApiConfiguration
import io.javalin.openapi.OpenApiAnnotationProcessorConfiguration
import io.javalin.openapi.OpenApiAnnotationProcessorConfigurer

@CompileStatic
@ExperimentalCompileOpenApiConfiguration
class OpenApiConfiguration implements OpenApiAnnotationProcessorConfigurer {

    @Override
    void configure(OpenApiAnnotationProcessorConfiguration openApiAnnotationProcessorConfiguration) {
        openApiAnnotationProcessorConfiguration.debug = true
    }

}