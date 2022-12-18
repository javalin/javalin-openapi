import groovy.transform.CompileStatic
import io.javalin.openapi.OpenApiAnnotationProcessorConfigurer

@CompileStatic
class OpenApiConfiguration implements OpenApiAnnotationProcessorConfigurer {

    @Override
    void configure() {
        // throw new RuntimeException("Panda")
    }

}