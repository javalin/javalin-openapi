package io.javalin.openapi.processor

import com.google.gson.JsonObject
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.processor.shared.JsonTypes.toModel
import io.javalin.openapi.processor.shared.ProcessorUtils.saveResource
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element

class JsonSchemaGenerator {

    fun generate(roundEnvironment: RoundEnvironment) =
        roundEnvironment.getElementsAnnotatedWith(JsonSchema::class.java)
            .filter { it.getAnnotation(JsonSchema::class.java).generateResource }
            .onEach { saveResource("json-schemes/${it}", generate(it)) }
            .run { saveResource("json-schemes/index", joinToString(separator = "\n")) }

    private fun generate(element: Element): String {
        val scheme = JsonObject()
        scheme.addProperty("\$schema", "http://json-schema.org/draft-07/schema#")

        val (entityScheme) = createTypeSchema(element.asType().toModel(), true)
        entityScheme.entrySet().forEach { (key, value) -> scheme.add(key, value) }

        return OpenApiAnnotationProcessor.gson.toJson(scheme)
    }

}