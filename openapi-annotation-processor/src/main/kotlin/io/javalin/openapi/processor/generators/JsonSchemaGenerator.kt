package io.javalin.openapi.processor.generators

import com.google.gson.JsonObject
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.processor.OpenApiAnnotationProcessor.Companion.filer
import io.javalin.openapi.processor.shared.JsonExtensions.toPrettyString
import io.javalin.openapi.processor.shared.JsonTypes.toModel
import io.javalin.openapi.processor.shared.saveResource
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element

class JsonSchemaGenerator {

    fun generate(roundEnvironment: RoundEnvironment) =
        roundEnvironment.getElementsAnnotatedWith(JsonSchema::class.java)
            .filter { it.getAnnotation(JsonSchema::class.java).generateResource }
            .onEach { filer.saveResource("json-schemes/${it}", generate(it)) }
            .run { filer.saveResource("json-schemes/index", joinToString(separator = "\n")) }

    private fun generate(element: Element): String {
        val scheme = JsonObject()
        scheme.addProperty("\$schema", "http://json-schema.org/draft-07/schema#")

        val (entityScheme) = createTypeSchema(element.asType().toModel(), true)
        entityScheme.entrySet().forEach { (key, value) -> scheme.add(key, value) }

        return scheme.toPrettyString()
    }

}