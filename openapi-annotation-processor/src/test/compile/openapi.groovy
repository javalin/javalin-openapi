import io.javalin.openapi.experimental.AnnotationProcessorContext
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.EmbeddedTypeProcessorContext
import io.javalin.openapi.experimental.ExperimentalCompileOpenApiConfiguration
import io.javalin.openapi.experimental.OpenApiAnnotationProcessorConfiguration
import io.javalin.openapi.experimental.OpenApiAnnotationProcessorConfigurer
import io.javalin.openapi.experimental.SimpleType
import org.jetbrains.annotations.Nullable

import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

@ExperimentalCompileOpenApiConfiguration
class OpenApiConfiguration implements OpenApiAnnotationProcessorConfigurer {

    @Override
    void configure(OpenApiAnnotationProcessorConfiguration configuration) {
        // configuration.debug = false

        // Used by TypeMappersTest
        configuration.simpleTypeMappings['io.javalin.openapi.processor.TypeMappersTest.CustomType'] = new SimpleType("string")

        // Used by UserCasesTest
        configuration.propertyInSchemeFilter = { AnnotationProcessorContext ctx, ClassDefinition type, Element property ->
            @Nullable TypeElement specificRecord = ctx.forTypeElement('io.javalin.openapi.processor.UserCasesTest.SpecificRecord')

            if (specificRecord != null && ctx.isAssignable(type.mirror, specificRecord.asType())) {
                return !ctx.hasElement(specificRecord, property)
            }

            return true
        }

        // Used by CustomTypeMappingsTest
        configuration.insertEmbeddedTypeProcessor({ EmbeddedTypeProcessorContext context ->
            if (context.type.simpleName == 'Optional' && context.type.generics.size() == 1) {
                context.parentContext.typeSchemaGenerator.addType(context.scheme, context.type.generics[0], context.inlineRefs, context.references, false)
                return true
            }

            return false
        })
    }

}