@file:Suppress("unused")

package io.javalin.openapi.processor

import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiName
import io.javalin.openapi.OpenApiNaming
import io.javalin.openapi.OpenApiNamingStrategy
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.experimental.processor.generators.splitCamelCase
import io.javalin.openapi.experimental.processor.generators.translatePropertyName
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class NamingStrategyTest : OpenApiAnnotationProcessorSpecification() {

    @Test
    fun should_split_camel_case_into_words() {
        assertThat(splitCamelCase("firstName")).containsExactly("first", "Name")
        assertThat(splitCamelCase("homeAddress")).containsExactly("home", "Address")
        assertThat(splitCamelCase("simple")).containsExactly("simple")
        assertThat(splitCamelCase("myURLParser")).containsExactly("my", "U", "R", "L", "Parser")
        assertThat(splitCamelCase("")).isEmpty()
    }

    @Test
    fun should_translate_property_names() {
        assertThat(translatePropertyName(OpenApiNamingStrategy.DEFAULT, "firstName")).isEqualTo("firstName")
        assertThat(translatePropertyName(OpenApiNamingStrategy.SNAKE_CASE, "firstName")).isEqualTo("first_name")
        assertThat(translatePropertyName(OpenApiNamingStrategy.SNAKE_CASE, "homeAddress")).isEqualTo("home_address")
        assertThat(translatePropertyName(OpenApiNamingStrategy.KEBAB_CASE, "firstName")).isEqualTo("first-name")
        assertThat(translatePropertyName(OpenApiNamingStrategy.KEBAB_CASE, "homeAddress")).isEqualTo("home-address")
        assertThat(translatePropertyName(OpenApiNamingStrategy.SNAKE_CASE, "simple")).isEqualTo("simple")
    }

    @OpenApiNaming(OpenApiNamingStrategy.SNAKE_CASE)
    private class SnakeCaseEntity(
        val firstName: String,
        val lastName: String,
        val homeAddress: String
    )

    @OpenApi(
        path = "/snake-case",
        versions = ["should_apply_snake_case_naming"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = SnakeCaseEntity::class)])]
    )
    @Test
    fun should_apply_snake_case_naming() = withOpenApi("should_apply_snake_case_naming") {
        assertThatJson(it)
            .inPath("$.components.schemas.SnakeCaseEntity.properties")
            .isObject
            .containsKey("first_name")
            .containsKey("last_name")
            .containsKey("home_address")
            .doesNotContainKey("firstName")
            .doesNotContainKey("lastName")
            .doesNotContainKey("homeAddress")
    }

    @OpenApiNaming(OpenApiNamingStrategy.KEBAB_CASE)
    private class KebabCaseEntity(
        val firstName: String,
        val lastName: String
    )

    @OpenApi(
        path = "/kebab-case",
        versions = ["should_apply_kebab_case_naming"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = KebabCaseEntity::class)])]
    )
    @Test
    fun should_apply_kebab_case_naming() = withOpenApi("should_apply_kebab_case_naming") {
        assertThatJson(it)
            .inPath("$.components.schemas.KebabCaseEntity.properties")
            .isObject
            .containsKey("first-name")
            .containsKey("last-name")
            .doesNotContainKey("firstName")
            .doesNotContainKey("lastName")
    }

    @OpenApiNaming(OpenApiNamingStrategy.SNAKE_CASE)
    private class NamingWithOverrideEntity(
        val firstName: String,
        @get:OpenApiName("customLastName")
        val lastName: String
    )

    @OpenApi(
        path = "/naming-override",
        versions = ["should_prefer_openapi_name_over_naming_strategy"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = NamingWithOverrideEntity::class)])]
    )
    @Test
    fun should_prefer_openapi_name_over_naming_strategy() = withOpenApi("should_prefer_openapi_name_over_naming_strategy") {
        assertThatJson(it)
            .inPath("$.components.schemas.NamingWithOverrideEntity.properties")
            .isObject
            .containsKey("first_name")
            .containsKey("customLastName")
            .doesNotContainKey("firstName")
            .doesNotContainKey("last_name")
    }

}
