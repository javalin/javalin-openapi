import io.javalin.openapi.experimental.AnnotationProcessorContext
import io.javalin.openapi.experimental.OpenApiType
import io.javalin.openapi.experimental.OpenApiTypeHandleKt
import io.javalin.openapi.experimental.EmbeddedTypeProcessorContext
import io.javalin.openapi.experimental.ExperimentalCompileOpenApiConfiguration
import io.javalin.openapi.experimental.OpenApiAnnotationProcessorConfiguration
import io.javalin.openapi.experimental.OpenApiAnnotationProcessorConfigurer
import io.javalin.openapi.experimental.SimpleType

import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

@ExperimentalCompileOpenApiConfiguration
class OpenApiConfiguration implements OpenApiAnnotationProcessorConfigurer {

    @Override
    void configure(OpenApiAnnotationProcessorConfiguration configuration) {
        configuration.validateWithParser = false

        configuration.simpleTypeMappings['io.javalin.openapi.processor.TypeMappersTest.CustomType'] = new SimpleType("string")

        configuration.propertyInSchemeFilter = { AnnotationProcessorContext ctx, OpenApiType type, Element property ->
            TypeElement filteredRecord = ctx.forTypeElement('io.javalin.openapi.processor.PropertySelectionTest.FilteredRecord')
            TypeElement filteredRecordBase = ctx.forTypeElement('io.javalin.openapi.processor.PropertySelectionTest.FilteredRecordBase')

            return [filteredRecord, filteredRecordBase].every { filteredType ->
                !ctx.isAssignable(OpenApiTypeHandleKt.getMirror(type), filteredType.asType()) || !ctx.hasElement(filteredType, property)
            }
        }

        configuration.insertEmbeddedTypeProcessor({ EmbeddedTypeProcessorContext context ->
            if (context.type.simpleName == 'AtomicReference' && context.type.generics.size() == 1) {
                context.parentContext.typeSchemaGenerator.addType(context.scheme, context.type.generics[0], context.inlineRefs, context.references, false)
                return true
            }

            return false
        })
    }

}
