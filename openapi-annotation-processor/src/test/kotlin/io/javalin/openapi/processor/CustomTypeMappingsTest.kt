@file:Suppress("unused")

package io.javalin.openapi.processor

import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test
import java.util.Optional

internal class CustomTypeMappingsTest : OpenApiAnnotationProcessorSpecification() {

    class EntityWithOptional(
        val text: Optional<String>  // it will be mapped by `compile/openapi.groovy` script
    )

    @OpenApi(
        path = "/optional",
        versions = ["should_map_optional_using_custom_mapping"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = EntityWithOptional::class)])]
    )
    @Test
    fun should_map_optional_using_custom_mapping() = withOpenApi("should_map_optional_using_custom_mapping") {
        println(it)

        assertThatJson(it)
            .inPath("$.components.schemas.EntityWithOptional")
            .isObject
            .isEqualTo(json("""
                {
                    "type": "object",
                    "additionalProperties": false,
                    "properties": {
                      "text": {
                        "type": "string"
                      }
                    },
                    "required": [
                      "text"
                    ]
                  }
                }
            """))
    }

}