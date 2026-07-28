@file:Suppress("unused")

package io.javalin.openapi.processor

import io.javalin.openapi.JsonSchema
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.experimental.processor.shared.jsonMapper
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class JsonSchemaTest : OpenApiAnnotationProcessorSpecification() {

    @JsonSchema(requireNonNulls = false)
    private class JsonSchemaWithoutRequired(val name: String, val age: Int)

    @Test
    fun should_honor_json_schema_require_non_nulls_false() = withJsonScheme("JsonSchemaWithoutRequired") {
        val document = jsonMapper.readTree(it)

        assertThat(document.path("\$schema").asText()).isEqualTo("https://json-schema.org/draft/2020-12/schema")
        assertThat(document.has("required")).isFalse()
        assertThat(document.path("properties").path("name").path("type").asText()).isEqualTo("string")
        assertThat(document.path("properties").path("age").path("type").asText()).isEqualTo("integer")
    }

    @JsonSchema(generateResource = false)
    private class DisabledJsonSchema(val ignored: String)

    @Test
    fun should_honor_json_schema_generate_resource_false() {
        val generatedNames = io.javalin.openapi.JsonSchemaLoader().loadGeneratedSchemes().map { it.name }

        assertThat(generatedNames).noneMatch { it.contains("DisabledJsonSchema") }
    }

    @JsonSchema
    private class NestedJsonSchema(val child: NestedJsonSchemaChild)

    private class NestedJsonSchemaChild(val value: String)

    @Test
    fun should_inline_nested_types_in_standalone_json_schema() = withJsonScheme("NestedJsonSchema") {
        assertThatJson(it)
            .inPath("$.properties.child")
            .isObject
            .isEqualTo(json("""
                {
                  "type": "object",
                  "properties": {
                    "value": {
                      "type": "string"
                    }
                  },
                  "required": ["value"]
                }
            """))
    }

    @JsonSchema
    private class RecursiveJsonSchema(val entities: List<RecursiveJsonSchemaEntity>)

    private class RecursiveJsonSchemaEntity(val schema: RecursiveJsonSchema)

    @Test
    fun should_use_local_references_for_recursive_standalone_json_schemas() = withJsonScheme("RecursiveJsonSchema") {
        val document = jsonMapper.readTree(it)
        val anchor = document.path($$"$anchor").asText()
        val recursiveReference = document
            .path("properties")
            .path("entities")
            .path("items")
            .path("properties")
            .path("schema")
            .path($$"$ref")
            .asText()

        assertThat(anchor).isNotBlank()
        assertThat(recursiveReference).isEqualTo("#$anchor")
    }

    @JsonSchema
    private class RecursiveGenericJsonSchema<T>(val child: RecursiveGenericJsonSchema<List<T>>)

    @Test
    fun should_handle_recursively_expanding_generic_json_schemas() = withJsonScheme("RecursiveGenericJsonSchema") {
        val document = jsonMapper.readTree(it)
        val anchor = document.path($$"$anchor").asText()
        val recursiveReference = document
            .path("properties")
            .path("child")
            .path($$"$ref")
            .asText()

        assertThat(anchor).isNotBlank()
        assertThat(recursiveReference).isEqualTo("#$anchor")
    }

    private class SharedNestedType(val value: String)

    private class OpenApiDocumentWithSharedNestedType(val nested: SharedNestedType)

    @JsonSchema
    private class JsonSchemaDocumentWithSharedNestedType(val nested: SharedNestedType)

    @OpenApi(
        path = "/shared-nested-type",
        versions = ["should_not_leak_openapi_references_into_json_schema"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = OpenApiDocumentWithSharedNestedType::class)])],
    )
    @Test
    fun should_not_leak_openapi_references_into_json_schema() = withJsonScheme("JsonSchemaDocumentWithSharedNestedType") {
        assertThatJson(it)
            .inPath("$.properties.nested")
            .isObject
            .doesNotContainKey("${'$'}ref")
            .containsEntry("type", "object")
            .containsEntry("properties", json("""{ "value": { "type": "string" } }"""))
    }

}
