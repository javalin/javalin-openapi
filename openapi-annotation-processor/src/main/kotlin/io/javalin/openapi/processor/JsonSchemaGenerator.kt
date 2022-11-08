package io.javalin.openapi.processor

import com.google.gson.JsonObject
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.processor.shared.JsonTypes.toModel
import io.javalin.openapi.processor.shared.ProcessorUtils
import javax.annotation.processing.FilerException
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.tools.StandardLocation

class JsonSchemaGenerator {

    fun generate(roundEnvironment: RoundEnvironment) =
        roundEnvironment.getElementsAnnotatedWith(JsonSchema::class.java)
            .filter { it.getAnnotation(JsonSchema::class.java).generateResource }
            .forEach { rawElement ->
                try {
                    val resource = OpenApiAnnotationProcessor.filer.createResource(StandardLocation.CLASS_OUTPUT, "", "json-schemes/${rawElement}")
                    val content = generate(rawElement)

                    resource.openWriter().use {
                        it.write(content)
                    }
                } catch (filerException: FilerException) {
                    // json-schemes/{this file} has been created during previous compilation phase
                } catch (throwable: Throwable) {
                    ProcessorUtils.printException(throwable)
                }
            }

    private fun generate(element: Element): String {
        val scheme = JsonObject()
        scheme.addProperty("\$schema", "http://json-schema.org/draft-07/schema#")

        val type = element.asType()
        val (entityScheme) = createTypeSchema(type.toModel(), true)
        entityScheme.entrySet().forEach { (key, value) -> scheme.add(key, value) }

        return OpenApiAnnotationProcessor.gson.toJson(scheme)
    }

}