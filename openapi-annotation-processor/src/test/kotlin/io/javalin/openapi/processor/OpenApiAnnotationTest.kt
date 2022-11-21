package io.javalin.openapi.processor

import io.javalin.openapi.HttpMethod.GET
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test

class OpenApiAnnotationTest : OpenApiAnnotationProcessorSpecification() {

    @OpenApi(
        path = "/",
        methods = [GET],
        versions = ["should generate info"]
    )
    @Test
    fun should_generate_info() = withOpenApi("should generate info") {
        assertThatJson(it)
            .isObject
            .containsEntry("openapi", "3.0.3")
            .containsEntry("info", json("""{ "title":"", "version": "" }"""))
    }

    @OpenApi(
        path = "/basic",
        methods = [GET],
        versions = ["should contain all basic properties from openapi annotation"],
        summary = "Test summary",
        operationId = "Test operation id",
        description = "Test description",
        deprecated = true,

    )
    @Test
    fun should_contain_all_basic_properties_from_openapi_annotation() = withOpenApi("should contain all basic properties from openapi annotation") {
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

}