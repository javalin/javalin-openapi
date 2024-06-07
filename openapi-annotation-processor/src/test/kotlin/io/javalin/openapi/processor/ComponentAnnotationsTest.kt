@file:Suppress("unused")

package io.javalin.openapi.processor

import io.javalin.openapi.Nullability.NULLABLE
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiDescription
import io.javalin.openapi.OpenApiNullable
import io.javalin.openapi.OpenApiPropertyType
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ComponentAnnotationsTest : OpenApiAnnotationProcessorSpecification() {

    @OpenApiDescription("Type description")
    private class ClassWithOpenApiDescription(
        @get:OpenApiDescription("Property description")
        val testProperty: String
    )

    @OpenApi(
        path = "/description",
        versions = ["should_include_openapi_description"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = ClassWithOpenApiDescription::class)])]
    )
    @Test
    fun should_include_openapi_description() = withOpenApi("should_include_openapi_description") {
        println(it)

        assertThatJson(it)
            .inPath("$.components.schemas.ClassWithOpenApiDescription")
            .isObject
            .containsEntry("description", "Type description")

        assertThatJson(it)
            .inPath("$.components.schemas.ClassWithOpenApiDescription.properties.testProperty")
            .isObject
            .containsEntry("description", "Property description")
    }

    private class ClassWithOpenApiType(
        @get:OpenApiPropertyType(definedBy = Double::class, nullability = NULLABLE)
        val testProperty: BigDecimal?
    )

    @OpenApi(
        path = "/type",
        versions = ["should_change_property_type"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = ClassWithOpenApiType::class)])]
    )
    @Test
    fun should_change_property_type() = withOpenApi("should_change_property_type") {
        println(it)

        assertThatJson(it)
            .inPath("$.components.schemas.ClassWithOpenApiType")
            .isObject
            .isEqualTo(json("""
                {
                    "type": "object",
                    "additionalProperties": false,
                    "properties": {
                        "testProperty": {
                            "type": "number",
                            "nullable": true,
                            "format": "double"
                        }
                    }
                }
            """))
    }

    private class ClassWithNullableProperty(
        @get:OpenApiNullable
        val testProperty: String
    )

    @OpenApi(
        path = "/nullable",
        versions = ["should_add_nullable_property"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = ClassWithNullableProperty::class)])]
    )
    @Test
    fun should_add_nullable_property() = withOpenApi("should_add_nullable_property") {
        println(it)

        assertThatJson(it)
            .inPath("$.components.schemas.ClassWithNullableProperty.properties.testProperty")
            .isObject
            .containsEntry("nullable", true)
    }

}
