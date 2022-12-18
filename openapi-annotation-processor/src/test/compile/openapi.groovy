import groovy.transform.CompileStatic
import io.javalin.openapi.experimental.AnnotationProcessorContext
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.ExperimentalCompileOpenApiConfiguration
import io.javalin.openapi.experimental.OpenApiAnnotationProcessorConfiguration
import io.javalin.openapi.experimental.OpenApiAnnotationProcessorConfigurer
import org.jetbrains.annotations.Nullable

import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

@CompileStatic
@ExperimentalCompileOpenApiConfiguration
class OpenApiConfiguration implements OpenApiAnnotationProcessorConfigurer {

    @Override
    void configure(OpenApiAnnotationProcessorConfiguration configuration) {
        configuration.debug = true

        configuration.propertyInSchemeFilter = { AnnotationProcessorContext ctx, ClassDefinition type, Element property ->
            @Nullable TypeElement specificRecord = ctx.forTypeElement('io.javalin.openapi.processor.UserCasesTest.SpecificRecord')

            if (ctx.isAssignable(type.mirror, specificRecord.asType())) {
                return !ctx.hasElement(specificRecord, property)
            }

            return true
        }
    }

}