package io.javalin.openapi.processor

import io.javalin.openapi.ContentType
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test
import java.io.Serializable

internal class SchemeTest : OpenApiAnnotationProcessorSpecification() {

    private open class BaseType {
        val baseProperty: String = "Test"
        val baseNested: BaseType.NestedClass = NestedClass()

        private inner class NestedClass {
            val nestedProperty: String = "Test"
        }
    }

    private class FinalClass : BaseType(), Serializable {
        val finalProperty: String = "Test"
        val finalNested: FinalClass.NestedClass = NestedClass()

        private inner class NestedClass {
            val nestedProperty: String = "Test"
        }
    }

    @OpenApi(
        path = "content-types",
        versions = ["should_generate_reference_with_inherited_properties"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = FinalClass::class, type = ContentType.JSON)])]
    )
    @Test
    fun should_generate_reference_with_inherited_properties() = withOpenApi("should_generate_reference_with_inherited_properties") {
        println(it)

        assertThatJson(it)
            .inPath("$.components.schemas.FinalClass.properties")
            .isObject
            .isEqualTo(json("""
                {
                  "baseProperty": {
                    "type": "string"
                  },
                  "baseNested": {
                    "${'$'}ref": "#/components/schemas/NestedClass"
                  },
                  "finalProperty": {
                    "type": "string"
                  },
                  "finalNested": {
                    "${'$'}ref": "#/components/schemas/NestedClass"
                  }
                }
            """))
    }

}