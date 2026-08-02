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
import java.util.concurrent.atomic.AtomicReference

internal class CustomTypeMappingsTest : OpenApiAnnotationProcessorSpecification() {

    class EntityWithAtomicReference(
        val value: AtomicReference<String>,
        val required: String,
    )

    @OpenApi(
        path = "/atomic-reference",
        versions = ["should_unwrap_atomic_reference_via_custom_processor"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = EntityWithAtomicReference::class)])]
    )
    @Test
    fun should_unwrap_atomic_reference_via_custom_processor() = withOpenApi("should_unwrap_atomic_reference_via_custom_processor") {
        assertThatJson(it)
            .inPath("$.components.schemas.EntityWithAtomicReference")
            .isObject
            .isEqualTo(json("""
                {
                    "type": "object",
                    "properties": {
                      "value": {
                        "type": "string"
                      },
                      "required": {
                        "type": "string"
                      }
                    },
                    "required": [
                      "value",
                      "required"
                    ]
                  }
                }
            """))
    }

    class EntityWithOptional(
        val text: Optional<String>,
        val required: String,
    )

    @OpenApi(
        path = "/optional",
        versions = ["should_unwrap_optional_as_nullable"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = EntityWithOptional::class)])]
    )
    @Test
    fun should_unwrap_optional_as_nullable() = withOpenApi("should_unwrap_optional_as_nullable") {
        assertThatJson(it)
            .inPath("$.components.schemas.EntityWithOptional")
            .isObject
            .isEqualTo(json("""
                {
                    "type": "object",
                    "properties": {
                      "text": {
                        "type": ["string", "null"]
                      },
                      "required": {
                        "type": "string"
                      }
                    },
                    "required": [
                      "text",
                      "required"
                    ]
                  }
                }
            """))
    }

    @OpenApi(
        path = "/primitive-redirect",
        versions = ["should_keep_primitive_redirect_required"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = PrimitiveRedirectDto::class)])]
    )
    @Test
    fun should_keep_primitive_redirect_required() = withOpenApi("should_keep_primitive_redirect_required") {
        assertThatJson(it)
            .inPath("$.components.schemas.PrimitiveRedirectDto.properties.createdAt")
            .isObject
            .isEqualTo(json("""{ "type": "integer", "format": "int64" }"""))

        assertThatJson(it)
            .inPath("$.components.schemas.PrimitiveRedirectDto.required")
            .isEqualTo(json("""["createdAt"]"""))
    }

}
