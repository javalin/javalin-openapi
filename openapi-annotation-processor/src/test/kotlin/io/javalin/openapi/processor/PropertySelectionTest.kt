@file:Suppress("unused")

package io.javalin.openapi.processor

import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiByFields
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiIgnore
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test

internal class PropertySelectionTest : OpenApiAnnotationProcessorSpecification() {

    private class IgnoreEntity(
        val visible: String,
        @get:OpenApiIgnore val hidden: String,
    )

    @OpenApi(
        path = "/ignore",
        versions = ["should_exclude_ignored_property"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = IgnoreEntity::class)])]
    )
    @Test
    fun should_exclude_ignored_property() = withOpenApi("should_exclude_ignored_property") {
        assertThatJson(it)
            .inPath("$.components.schemas.IgnoreEntity.properties")
            .isObject
            .containsKey("visible")
            .doesNotContainKey("hidden")
    }

    @OpenApiByFields
    private class ByFieldsEntity {
        @JvmField val fieldProperty: String = ""
        val getterProperty: String = ""
    }

    @OpenApi(
        path = "/by-fields",
        versions = ["should_include_public_fields_with_by_fields"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = ByFieldsEntity::class)])]
    )
    @Test
    fun should_include_public_fields_with_by_fields() = withOpenApi("should_include_public_fields_with_by_fields") {
        assertThatJson(it)
            .inPath("$.components.schemas.ByFieldsEntity.properties")
            .isObject
            .containsKey("fieldProperty")
            .containsKey("getterProperty")
    }

}
