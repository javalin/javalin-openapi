@file:Suppress("unused")

package io.javalin.openapi.processor

import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiArrayValidation
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiNumberValidation
import io.javalin.openapi.OpenApiObjectValidation
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.OpenApiStringValidation
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test

internal class ValidationAnnotationsTest : OpenApiAnnotationProcessorSpecification() {

    private class ValidatedEntity(
        @get:OpenApiNumberValidation(minimum = "1", maximum = "10", exclusiveMinimum = "0", exclusiveMaximum = "11", multipleOf = "2")
        val score: Int,
        @get:OpenApiStringValidation(minLength = "2", maxLength = "8", format = "email", pattern = "^[a-z]+$")
        val name: String,
        @get:OpenApiArrayValidation(minItems = "1", maxItems = "5", uniqueItems = true)
        val tags: List<String>,
        @get:OpenApiObjectValidation(minProperties = "1", maxProperties = "3")
        val meta: Map<String, String>,
    )

    @OpenApi(
        path = "/validations",
        versions = ["should_emit_validation_keywords"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = ValidatedEntity::class)])]
    )
    @Test
    fun should_emit_validation_keywords() = withOpenApi("should_emit_validation_keywords") {
        val properties = "$.components.schemas.ValidatedEntity.properties"

        assertThatJson(it).inPath("$properties.score").isObject.isEqualTo(json("""
            { "type": "integer", "format": "int32", "minimum": 1, "maximum": 10, "exclusiveMinimum": 0, "exclusiveMaximum": 11, "multipleOf": 2 }
        """))

        assertThatJson(it).inPath("$properties.name").isObject.isEqualTo(json("""
            { "type": "string", "minLength": 2, "maxLength": 8, "format": "email", "pattern": "^[a-z]+$" }
        """))

        assertThatJson(it).inPath("$properties.tags").isObject.isEqualTo(json("""
            { "type": "array", "items": { "type": "string" }, "minItems": 1, "maxItems": 5, "uniqueItems": true }
        """))

        assertThatJson(it).inPath("$properties.meta").isObject.isEqualTo(json("""
            { "type": "object", "additionalProperties": { "type": "string" }, "minProperties": 1, "maxProperties": 3 }
        """))
    }

}
