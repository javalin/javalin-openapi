package io.javalin.openapi.processor.generators

import com.google.gson.JsonObject
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.experimental.processor.shared.saveResource
import io.javalin.openapi.experimental.processor.shared.toPrettyString
import io.javalin.openapi.processor.OpenApiAnnotationProcessor.Companion.context
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element

class JsonSchemaGenerator {

    fun generate(roundEnvironment: RoundEnvironment) =
        roundEnvironment.getElementsAnnotatedWith(JsonSchema::class.java)
            .filter { it.getAnnotation(JsonSchema::class.java).generateResource }
            .onEach { context.env.filer.saveResource(context, "json-schemes/${it}", generate(it)) }
            .run { context.env.filer.saveResource(context, "json-schemes/index", joinToString(separator = "\n")) }

    private fun generate(element: Element): String {
        val scheme = JsonObject()
        scheme.addProperty("\$schema", "http://json-schema.org/draft-07/schema#")

        context.inContext {
            val (entityScheme) = context.typeSchemaGenerator.createTypeSchema(
                type = element.asType().toClassDefinition(),
                inlineRefs = true
            )

            entityScheme.entrySet().forEach { (key, value) ->
                scheme.add(key, value)
            }
        }

        return scheme.toPrettyString()
    }

}