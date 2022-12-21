package io.javalin.openapi.processor.specification

import io.javalin.openapi.JsonSchemaLoader
import io.javalin.openapi.OpenApiLoader
import org.junit.jupiter.api.Assertions

typealias SchemeConsumer = (String) -> Unit

internal abstract class OpenApiAnnotationProcessorSpecification {

    private companion object {
        val openApiSchemes = OpenApiLoader().loadOpenApiSchemes()
        val jsonSchemes = JsonSchemaLoader().loadGeneratedSchemes()
    }


    fun withOpenApi(name: String, consumer: SchemeConsumer) {
        openApiSchemes[name.replace(" ", "-")]
            ?.also { consumer(it) }
            ?: failWithSchemeNotFound(name)
    }

    fun withJsonScheme(name: String, consumer: SchemeConsumer) {
        jsonSchemes
            .find { it.name.contains(name) }
            ?.also { consumer(it.getContentAsString()) }
            ?: failWithSchemeNotFound(name)
    }

    private fun failWithSchemeNotFound(name: String) {
        Assertions.fail<Nothing>("Scheme '$name' not found")
    }

}
