@file:Suppress("unused")

package io.javalin.openapi.processor

import io.javalin.openapi.*
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import java.io.File
import java.io.InputStream
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

internal class TypeMappersTest : OpenApiAnnotationProcessorSpecification() {

    class CustomType // mapped by openapi.groovy

    enum class StandardEnum {
        VALUE_1,
        VALUE_2,
    }

    enum class CapitalizedFirstLetterEnum {
        Value1,
        Value2,
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "RemoveRedundantQualifierName")
    class SimpleTypesList(
        val customType: CustomType,
        val boolean: Boolean,
        val booleanObject: java.lang.Boolean,
        val byte: Byte,
        val byteObject: java.lang.Byte,
        val short: Short,
        val shortObject: java.lang.Short,
        val int: Int,
        val intObject: Integer,
        val long: Long,
        val longObject: java.lang.Long,
        val float: Float,
        val floatObject: java.lang.Float,
        val double: Double,
        val doubleObject: java.lang.Double,
        val char: Char,
        val charObject: java.lang.Character,
        val string: String,
        val bigDecimal: BigDecimal,
        val uuid: UUID,
        val objectId: ObjectId,
        val byteArray: ByteArray,
        val inputStream: InputStream,
        val file: File,
        val date: Date,
        val localDate: LocalDate,
        val localDateTime: LocalDateTime,
        val instant: Instant,
        val obj: Object,
        val map: Map<*, *>,
        val mapWithList: Map<*, List<*>>,
        val standardEnum: StandardEnum,
        val capitalizedFirstLetterEnum: CapitalizedFirstLetterEnum,
    )

    @OpenApi(
        path = "simple-types",
        versions = ["should_map_all_simple_types"],
        responses = [
            OpenApiResponse(
                status = "200",
                content = [
                    OpenApiContent(from = SimpleTypesList::class),
                ],
            ),
        ]
    )
    @Test
    fun should_map_all_simple_types() = withOpenApi("should_map_all_simple_types") {
        println(it)

        assertThatJson(it)
            .inPath("$.components.schemas.SimpleTypesList.properties")
            .isObject
            .isEqualTo(json(
                // language=json
                """
                {
                  "customType": {
                    "type": "string"
                  },
                  "boolean": {
                    "type": "boolean"
                  },
                  "booleanObject": {
                    "type": "boolean"
                  },
                  "byte": {
                    "type": "integer",
                    "format": "int32"
                  },
                  "byteObject": {
                    "type": "integer",
                    "format": "int32"
                  },
                  "short": {
                    "type": "integer",
                    "format": "int32"
                  },
                  "shortObject": {
                    "type": "integer",
                    "format": "int32"
                  },
                  "int": {
                    "type": "integer",
                    "format": "int32"
                  },
                  "intObject": {
                    "type": "integer",
                    "format": "int32"
                  },
                  "long": {
                    "type": "integer",
                    "format": "int64"
                  },
                  "longObject": {
                    "type": "integer",
                    "format": "int64"
                  },
                  "float": {
                    "type": "number",
                    "format": "float"
                  },
                  "floatObject": {
                    "type": "number",
                    "format": "float"
                  },
                  "double": {
                    "type": "number",
                    "format": "double"
                  },
                  "doubleObject": {
                    "type": "number",
                    "format": "double"
                  },
                  "char": {
                    "type": "string"
                  },
                  "charObject": {
                    "type": "string"
                  },
                  "string": {
                    "type": "string"
                  },
                  "bigDecimal": {
                    "type": "string",
                    "format": "decimal"
                  },
                  "uuid": {
                    "type": "string",
                    "format": "uuid"
                  },
                  "objectId": {
                    "type": "string"
                  },
                  "byteArray": {
                    "type": "string",
                    "format": "binary"
                  },
                  "inputStream": {
                    "type": "string",
                    "format": "binary"
                  },
                  "file": {
                    "type": "string",
                    "format": "binary"
                  },
                  "date": {
                    "type": "string",
                    "format": "date"
                  },
                  "localDate": {
                    "type": "string",
                    "format": "date"
                  },
                  "localDateTime": {
                    "type": "string",
                    "format": "date-time"
                  },
                  "instant": {
                    "type": "string",
                    "format": "date-time"
                  },
                  "obj": {
                    "type": "object"
                  },
                  "map": {
                    "type": "object",
                    "additionalProperties": {
                      "type": "object"
                    }
                  },
                  "mapWithList": {
                    "type": "object",
                    "additionalProperties": {
                      "type": "array",
                      "items": {
                        "type": "object"
                      }
                    }
                  },
                  "standardEnum":{"${'$'}ref":"#/components/schemas/StandardEnum"},
                  "capitalizedFirstLetterEnum":{"${'$'}ref":"#/components/schemas/CapitalizedFirstLetterEnum"}
                }
                """
            ))
    }

    @OpenApi(
        path = "dictionary-structure",
        versions = ["should_output_dictionary_structure"],
        responses = [
            OpenApiResponse(
                status = "200",
                content = [
                    OpenApiContent(
                        mimeType = "application/map-string-string",
                        additionalProperties = OpenApiAdditionalContent(
                            from = String::class,
                            exampleObjects = [OpenApiExampleProperty(name = "monke", value = "banana")]
                        )
                    ),
                ],
            ),
        ]
    )
    @Test
    fun should_output_dictionary_structure() = withOpenApi("should_output_dictionary_structure") {
        println(it)

        assertThatJson(it)
            .inPath("$.paths['/dictionary-structure'].get.responses.200.content['application/map-string-string'].schema")
            .isObject
            .isEqualTo(json(
                // language=json
                """
                {
                  "type": "object",
                  "additionalProperties": {
                    "type": "string"
                  },
                  "example": {
                    "monke": "banana"
                  }
                }
                """,
            ))
    }

    @OpenApi(
        path = "nested-list-example",
        versions = ["should_support_nested_lists_in_example_objects"],
        responses = [
            OpenApiResponse(
                status = "200",
                content = [
                    OpenApiContent(
                        from = String::class,
                        exampleObjects = [
                            OpenApiExampleProperty(name = "name", value = "document"),
                            OpenApiExampleProperty(
                                name = "metadata",
                                objects = [
                                    OpenApiExampleProperty(name = "source", value = "document"),
                                    OpenApiExampleProperty(name = "author", value = "John Doe"),
                                    OpenApiExampleProperty(
                                        name = "tags",
                                        objects = [
                                            OpenApiExampleProperty(value = "important"),
                                            OpenApiExampleProperty(value = "research")
                                        ]
                                    )
                                ]
                            )
                        ]
                    ),
                ],
            ),
        ]
    )
    @Test
    fun should_support_nested_lists_in_example_objects() = withOpenApi("should_support_nested_lists_in_example_objects") {
        println(it)

        assertThatJson(it)
            .inPath("$.paths['/nested-list-example'].get.responses.200.content['text/plain'].example")
            .isObject
            .isEqualTo(json(
                // language=json
                """
                {
                  "name": "document",
                  "metadata": {
                    "source": "document",
                    "author": "John Doe",
                    "tags": ["important", "research"]
                  }
                }
                """
            ))
    }

    private class RawExampleEntity(
        @get:OpenApiExample(raw = "1234")
        val intField: Int,
        @get:OpenApiExample(raw = "true")
        val boolField: Boolean,
        @get:OpenApiExample(raw = """[1, 2, 3]""")
        val arrayField: List<Int>,
        @get:OpenApiExample(value = "string-example")
        val stringField: String
    )

    @OpenApi(
        path = "raw-examples",
        versions = ["should_support_raw_examples"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = RawExampleEntity::class)])]
    )
    @Test
    fun should_support_raw_examples() = withOpenApi("should_support_raw_examples") {
        println(it)

        assertThatJson(it)
            .inPath("$.components.schemas.RawExampleEntity.properties.intField.example")
            .isEqualTo(1234)

        assertThatJson(it)
            .inPath("$.components.schemas.RawExampleEntity.properties.boolField.example")
            .isEqualTo(true)

        assertThatJson(it)
            .inPath("$.components.schemas.RawExampleEntity.properties.arrayField.example")
            .isEqualTo(json("[1, 2, 3]"))

        assertThatJson(it)
            .inPath("$.components.schemas.RawExampleEntity.properties.stringField.example")
            .isEqualTo("string-example")
    }

    @OpenApiPropertyType(definedBy = Int::class)
    @OpenApiDescription("Sort order:\n * 1 - Request type 1\n * 2 - Request type 2")
    private enum class IntegerEnum {
        @OpenApiName("1") REQUEST_TYPE_1,
        @OpenApiName("2") REQUEST_TYPE_2
    }

    @OpenApi(
        path = "integer-enum",
        versions = ["should_support_integer_enum"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = IntegerEnum::class)])]
    )
    @Test
    fun should_support_integer_enum() = withOpenApi("should_support_integer_enum") {
        println(it)

        assertThatJson(it)
            .inPath("$.components.schemas.IntegerEnum")
            .isObject
            .isEqualTo(json(
                // language=json
                """
                {
                  "type": "integer",
                  "format": "int32",
                  "enum": [1, 2],
                  "description": "Sort order:\n * 1 - Request type 1\n * 2 - Request type 2"
                }
                """
            ))
    }

    @OpenApiDescription("A regular string enum with description")
    private enum class DescribedStringEnum {
        ALPHA,
        BETA
    }

    @OpenApi(
        path = "described-enum",
        versions = ["should_support_description_on_string_enum"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = DescribedStringEnum::class)])]
    )
    @Test
    fun should_support_description_on_string_enum() = withOpenApi("should_support_description_on_string_enum") {
        assertThatJson(it)
            .inPath("$.components.schemas.DescribedStringEnum")
            .isObject
            .isEqualTo(json(
                // language=json
                """
                {
                  "type": "string",
                  "enum": ["ALPHA", "BETA"],
                  "description": "A regular string enum with description"
                }
                """
            ))
    }

    private class Loop(
        val self: Loop?,
    )

    @OpenApi(
        path = "recursive",
        versions = ["should_map_recursive_type"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = Loop::class)])]
    )
    @Test
    fun should_map_recursive_type() = withOpenApi("should_map_recursive_type") {
        assertThatJson(it)
            .inPath("$.components.schemas.Loop.properties.self.anyOf")
            .isArray
            .hasSize(2)

        assertThatJson(it)
            .inPath("$.components.schemas.Loop.properties.self.anyOf[0]")
            .isObject
            .containsEntry($$"$ref", "#/components/schemas/Loop")

        assertThatJson(it)
            .inPath("$.components.schemas.Loop.properties.self.anyOf[1]")
            .isObject
            .containsEntry("type", "null")
    }
}
