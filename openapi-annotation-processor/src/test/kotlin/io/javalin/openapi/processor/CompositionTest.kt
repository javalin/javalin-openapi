@file:Suppress("unused")

package io.javalin.openapi.processor

import io.javalin.openapi.Custom
import io.javalin.openapi.Discriminator
import io.javalin.openapi.DiscriminatorMappingName
import io.javalin.openapi.DiscriminatorProperty
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.OneOf
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiName
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.Test

internal class CompositionTest : OpenApiAnnotationProcessorSpecification() {

    interface Storage

    class FileSystemStorage(
        @get:Custom(name = "const", "fs")
        val type: String = "fs"
    ) : Storage

    class S3Storage(
        @get:Custom(name = "const", "s3")
        val type: String = "s3"
    ) : Storage

    @JsonSchema
    class SomeConfiguration(
        @get:OneOf(FileSystemStorage::class, S3Storage::class)
        val storage: Storage
    )

    @Test
    fun should_generate_valid_json_scheme_with_composition_property() = withJsonScheme(SomeConfiguration::class.java.canonicalName) {
        assertThatJson(it)
            .inPath("properties.storage")
            .isObject
            .isEqualTo(json("""
                {
                    "oneOf": [
                        {
                          "type": "object",
                          "additionalProperties": false,
                          "properties": {
                            "type": {
                              "type": "string",
                              "const": "fs"
                            }
                          },
                          "required": [
                            "type"
                          ]
                        },
                        {
                          "type": "object",
                          "additionalProperties": false,
                          "properties": {
                            "type": {
                              "type": "string",
                              "const": "s3"
                            }
                          },
                          "required": [
                            "type"
                          ]
                        }
                    ]
                }
            """))
    }

    @OneOf(
        discriminator = Discriminator(
            property = DiscriminatorProperty(
                name = "type",
                type = String::class,
                injectInMappings = true
            )
        )
    )
    sealed interface Union

    @DiscriminatorMappingName("class-a")
    data class A(val a: Int) : Union

    @DiscriminatorMappingName("class-b")
    @OpenApiName("B")
    data class C(val b: String) : Union

    @OpenApi(
        path = "discriminator",
        versions = ["should_resolve_subtypes_as_mappings"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = Union::class)])]
    )
    @Test
    fun should_resolve_subtypes_as_mappings() = withOpenApi("should_resolve_subtypes_as_mappings") {
        println(it)

        assertThatJson(it)
            .inPath("$.components.schemas.Union")
            .isObject
            .isEqualTo(json("""
                {
                    "oneOf": [
                      {
                        "${'$'}ref": "#/components/schemas/A"
                      },
                      {
                        "${'$'}ref": "#/components/schemas/B"
                      }
                    ],
                    "discriminator": {
                      "propertyName": "type",
                      "mappings": {
                        "class-a": "#/components/schemas/A",
                        "class-b": "#/components/schemas/B"
                      }
                    }
                }
            """))

        assertThatJson(it)
            .inPath("$.components.schemas.A")
            .isObject
            .isEqualTo(json("""
                {
                    "type": "object",
                    "additionalProperties": false,
                    "properties": {
                      "a": {
                        "type": "integer",
                        "format": "int32"
                      },
                      "type": {
                        "type": "string"
                      }
                    },
                    "required": [
                      "a",
                      "type"
                    ]
                  }
            """))

        assertThatJson(it)
            .inPath("$.components.schemas.B")
            .isObject
            .isEqualTo(json("""
                {
                    "type": "object",
                    "additionalProperties": false,
                    "properties": {
                      "b": {
                        "type": "string"
                      },
                      "type": {
                        "type": "string"
                      }
                    },
                    "required": [
                      "b",
                      "type"
                    ]
                  }
            """))
    }

}