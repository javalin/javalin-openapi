@file:Suppress("unused")

package io.javalin.openapi.processor

import io.javalin.openapi.Custom
import io.javalin.openapi.CustomAnnotation
import io.javalin.openapi.HttpMethod.GET
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiDescription
import io.javalin.openapi.OpenApiName
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.JsonAssertions.value
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test

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

}