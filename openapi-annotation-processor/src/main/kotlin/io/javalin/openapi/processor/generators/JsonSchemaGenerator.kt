package io.javalin.openapi.processor.generators

import io.javalin.openapi.JsonSchema
import io.javalin.openapi.experimental.processor.shared.saveResource
import io.javalin.openapi.processor.OpenApiAnnotationProcessor.Companion.context
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element

class JsonSchemaGenerator {

    fun generate(roundEnvironment: RoundEnvironment) {
        val elements = roundEnvironment.getElementsAnnotatedWith(JsonSchema::class.java)
            .filter { it.getAnnotation(JsonSchema::class.java)!!.generateResource }

        for (element in elements) {
            context.env.filer.saveResource(context, "json-schemes/$element", generate(element))
        }

        context.env.filer.saveResource(context, "json-schemes/index", elements.joinToString(separator = "\n"))
    }

    private fun generate(element: Element): String =
        with(context) {
            typeSchemaGenerator.createTypeSchema(
                type = element.asType().toOpenApiType(),
                inlineRefs = true,
            ).toJsonSchemaString()
        }

}
