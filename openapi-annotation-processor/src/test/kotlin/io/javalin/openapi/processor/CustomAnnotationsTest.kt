@file:Suppress("unused")

package io.javalin.openapi.processor

import io.javalin.openapi.Custom
import io.javalin.openapi.CustomAnnotation
import io.javalin.openapi.HttpMethod.GET
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiName
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER

internal class CustomAnnotationsTest : OpenApiAnnotationProcessorSpecification() {

    @CustomAnnotation
    @Target(CLASS)
    private annotation class CustomAnnotationOnClass(val onClass: BooleanArray)

    @CustomAnnotation
    @Target(PROPERTY_GETTER)
    private annotation class CustomAnnotationOnGetter(val onGetter: BooleanArray)

    @CustomAnnotation
    @Target(PROPERTY_GETTER)
    private annotation class Schema(
        val allowableValues: Array<String> = [],
        val description: String = "",
        val example: String = "",
        val format: String = "",
        val pattern: String = "",
    )

    @Custom(name = "description", value = "Custom description")
    @CustomAnnotationOnClass(onClass = [true])
    private class CustomEntity(
        @get:CustomAnnotationOnGetter(onGetter = [true])
        val element: Map<String, Map<String, CustomEntity>>
    )

    @OpenApi(
        path = "/custom",
        versions = ["should_include_custom_annotation_in_type_scheme"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = CustomEntity::class)])]
    )
    @Test
    fun should_include_custom_annotation_in_type_scheme() = withOpenApi("should_include_custom_annotation_in_type_scheme") {
        println(it)

        assertThatJson(it)
            .inPath("$.paths['/custom'].get.responses.200.content['application/json'].schema")
            .isObject
            .containsEntry($$"$ref", "#/components/schemas/CustomEntity")

        assertThatJson(it)
            .inPath("$.components.schemas.CustomEntity")
            .isObject
            .containsEntry("onClass", json("[true]"))
            .containsEntry("description", "Custom description")

        assertThatJson(it)
            .inPath("$.components.schemas.CustomEntity.properties.element")
            .isObject
            .containsEntry("onGetter", json("[true]"))
    }

    private data class KeypairCreateResponse(
        @get:Schema(
            allowableValues = ["valid", "expired", "revoked"],
            format = "fingerprint",
            pattern = "^(valid|expired|revoked)$",
            description = "status of the key like valid|expired|revoked",
        )
        val fingerprint: String,
    )

    @OpenApi(
        path = "/custom-annotation-array",
        versions = ["should_include_array_values_from_custom_annotations"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = KeypairCreateResponse::class)])],
    )
    @Test
    fun should_include_array_values_from_custom_annotations() =
        withOpenApi("should_include_array_values_from_custom_annotations") {
        assertThatJson(it)
            .inPath("$.components.schemas.KeypairCreateResponse.properties.fingerprint")
            .isObject
            .containsEntry("allowableValues", json("[\"valid\", \"expired\", \"revoked\"]"))
            .containsEntry("description", "status of the key like valid|expired|revoked")
            .containsEntry("format", "fingerprint")
            .containsEntry("pattern", "^(valid|expired|revoked)$")
    }

    @OpenApiName("PandaEntity")
    private class OpenApiNameEntity

    @OpenApi(
        path = "name",
        methods = [GET],
        versions = ["should_rename_entity"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = OpenApiNameEntity::class)])]
    )
    @Test
    fun should_rename_entity() = withOpenApi("should_rename_entity") {
        assertThatJson(it)
            .inPath("$.paths['/name'].get.responses.200.content['application/json'].schema")
            .isObject
            .containsEntry($$"$ref", "#/components/schemas/PandaEntity")

        assertThatJson(it)
            .inPath("$.components.schemas.PandaEntity")
            .isObject
    }

    @OpenApi(
        path = "/inherited-custom-annotation",
        versions = ["should_include_inherited_custom_annotation_extras"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = InheritedExtraChild::class)])]
    )
    @Test
    fun should_include_inherited_custom_annotation_extras() = withOpenApi("should_include_inherited_custom_annotation_extras") {
        assertThatJson(it)
            .inPath("$.components.schemas.InheritedExtraChild")
            .isObject
            .containsEntry("inherited", "yes")
    }

    @OpenApi(
        path = "/nested-custom-annotation",
        versions = ["should_emit_nested_custom_annotation_values_as_json"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = NestedExtraDto::class)])]
    )
    @Test
    fun should_emit_nested_custom_annotation_values_as_json() = withOpenApi("should_emit_nested_custom_annotation_values_as_json") {
        assertThatJson(it)
            .inPath("$.components.schemas.NestedExtraDto.nested")
            .isEqualTo(json("""{ "note": "x" }"""))
    }

}
