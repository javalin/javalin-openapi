@file:Suppress("unused")

package io.javalin.openapi.processor

import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
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
        val intObject: java.lang.Integer,
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
        val map: Map<*, *>
    )

    @OpenApi(
        path = "simple-types",
        versions = ["should_map_all_simple_types"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = SimpleTypesList::class)])]
    )
    @Test
    fun should_map_all_simple_types() = withOpenApi("should_map_all_simple_types") {
        println(it)

        assertThatJson(it)
            .inPath("$.components.schemas.SimpleTypesList.properties")
            .isObject
            .isEqualTo(json("""
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
                    "type": "string"
                  },
                  "uuid": {
                    "type": "string"
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
                  }
                }"""
            ))
    }

}