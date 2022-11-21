package io.javalin.openapi

import org.junit.jupiter.api.Assertions

abstract class OpenApiAnnotationProcessorSpecification {

    companion object {
        val openApiSchemes = OpenApiLoader().loadOpenApiSchemes()
    }

    fun withOpenApi(name: String, consumer: (String) -> Unit) {
        openApiSchemes[name.replace(" ", "-")]
            ?.also { consumer(it) }
            ?: run { Assertions.fail("Scheme '$name' not found") }
    }

}