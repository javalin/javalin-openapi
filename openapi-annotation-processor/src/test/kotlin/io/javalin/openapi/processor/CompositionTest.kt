@file:Suppress("unused")

package io.javalin.openapi.processor

import io.javalin.openapi.Discriminator
import io.javalin.openapi.DiscriminatorMappingName
import io.javalin.openapi.OneOf
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import org.junit.jupiter.api.Test

internal class CompositionTest : OpenApiAnnotationProcessorSpecification() {

    @OneOf(
        discriminator = Discriminator(
            propertyName = "type"
        )
    )
    sealed interface Union

    @DiscriminatorMappingName("class-a")
    data class A(val a: Int) : Union

    @DiscriminatorMappingName("class-b")
    data class B(val b: String) : Union

    @OpenApi(
        path = "discriminator",
        versions = ["should_resolve_subtypes_as_mappings"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = Union::class)])]
    )
    @Test
    fun should_resolve_subtypes_as_mappings() = withOpenApi("should_resolve_subtypes_as_mappings") {
        println(it)

//        assertThatJson(it)
//            .inPath("$.components.schemas.SimpleTypesList.properties")
//            .isObject
//            .isEqualTo(json("""
//                {
//                  "customType": {
//                    "type": "string"
//                  },
//                  "map": {
//                    "type": "object",
//                    "additionalProperties": {
//                      "type": "object"
//                    }
//                  }
//                }"""
//            ))
    }

}