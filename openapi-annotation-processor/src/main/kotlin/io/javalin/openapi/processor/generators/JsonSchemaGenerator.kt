package io.javalin.openapi.processor.generators

import io.javalin.openapi.JsonSchema
import io.javalin.openapi.experimental.processor.shared.saveResource
import io.javalin.openapi.processor.OpenApiAnnotationProcessor.Companion.context
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element

class JsonSchemaGenerator {

    fun generate(roundEnvironment: RoundEnvironment) =
        roundEnvironment.getElementsAnnotatedWith(JsonSchema::class.java)
            .filter { it.getAnnotation(JsonSchema::class.java).generateResource }
            .onEach { context.env.filer.saveResource(context, "json-schemes/${it}", generate(it)) }
            .run { context.env.filer.saveResource(context, "json-schemes/index", joinToString(separator = "\n")) }

    private fun generate(element: Element): String =
        context.inContext {
            context.typeSchemaGenerator.createTypeSchema(
                type = element.asType().toClassDefinition(),
                inlineRefs = true
            ).toJsonSchemaString()
        }

}
