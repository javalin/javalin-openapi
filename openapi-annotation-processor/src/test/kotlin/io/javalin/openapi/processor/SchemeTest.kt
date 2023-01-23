package io.javalin.openapi.processor

import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test

internal class SchemeTest : OpenApiAnnotationProcessorSpecification() {

    private open class BaseType {
        val baseField: String = "Test"
    }

    private class FinalClass : BaseType() {
        val finalField: String = "Test"
    }

    @OpenApi(
        path = "content-types",
        versions = ["should_generate_reference_with_inherited_properties"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = FinalClass::class)])]
    )
    @Test
    fun should_generate_reference_with_inherited_properties() = withOpenApi("should_generate_reference_with_inherited_properties") {
        assertThatJson(it)
            .inPath("$.components.schemas.FinalClass.properties")
            .isObject
            .isEqualTo(json("""
                {
                  "baseField": {
                    "type": "string"
                  },
                  "finalField": {
                    "type": "string"
                  }
                }
            """))
    }

}