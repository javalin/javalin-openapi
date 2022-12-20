@file:Suppress("unused")

package io.javalin.openapi.processor

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
            .containsEntry("\$ref", "#/components/schemas/CustomEntity")

        assertThatJson(it)
            .inPath("$.components.schemas.CustomEntity")
            .isObject
            .containsEntry("onClass", json("[true]"))

        assertThatJson(it)
            .inPath("$.components.schemas.CustomEntity.properties.element")
            .isObject
            .containsEntry("onGetter", json("[true]"))
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
            .containsEntry("\$ref", "#/components/schemas/PandaEntity")

        assertThatJson(it)
            .inPath("$.components.schemas.PandaEntity")
            .isObject
    }

}