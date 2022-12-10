package io.javalin.openapi.processor

import io.javalin.openapi.CustomAnnotation
import io.javalin.openapi.HttpMethod.GET
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiAnnotationProcessorSpecification
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test

class OpenApiAnnotationTest : OpenApiAnnotationProcessorSpecification() {

    @OpenApi(
        path = "/",
        methods = [GET],
        versions = ["should_generate_info"]
    )
    @Test
    fun should_generate_info() = withOpenApi("should_generate_info") {
        assertThatJson(it)
            .isObject
            .containsEntry("openapi", "3.0.3")
            .containsEntry("info", json("""{ "title":"", "version": "" }"""))
    }

    @OpenApi(
        path = "basic",
        methods = [GET],
        versions = ["should_contain_all_basic_properties_from_openapi_annotation"],
        summary = "Test summary",
        operationId = "Test operation id",
        description = "Test description",
        deprecated = true,
    )
    @Test
    fun should_contain_all_basic_properties_from_openapi_annotation() = withOpenApi("should_contain_all_basic_properties_from_openapi_annotation") {
        assertThatJson(it)
            .inPath("$.paths['/basic'].get")
            .isObject
            .containsAllEntriesOf(linkedMapOf(
                "tags" to json("[]"),
                "summary" to "Test summary",
                "description" to "Test description",
                "operationId" to "Test operation id",
                "parameters" to json("[]"),
                "deprecated" to true,
                "security" to json("[]")
            ))
    }

    @CustomAnnotation
    private annotation class CustomDescription(
        val customName: String
    )

    @CustomDescription(customName = "Monke")
    private class CustomEntity

    @OpenApi(
        path = "custom",
        methods = [GET],
        versions = ["should_include_custom_annotation_in_type_scheme"],
        responses = [
            OpenApiResponse(
                status = "200",
                content = [OpenApiContent(from = CustomEntity::class)]
            )
        ]
    )
    @Test
    fun should_include_custom_annotation_in_type_scheme() = withOpenApi("should_include_custom_annotation_in_type_scheme") {
        assertThatJson(it)
            .inPath("$.paths['/custom'].get.responses.200.content['application/json'].schema")
            .isObject
            .containsEntry("\$ref", "#/components/schemas/CustomEntity")

        assertThatJson(it)
            .inPath("$.components.schemas.CustomEntity")
            .isObject
            .containsEntry("customName", "Monke")
    }

}