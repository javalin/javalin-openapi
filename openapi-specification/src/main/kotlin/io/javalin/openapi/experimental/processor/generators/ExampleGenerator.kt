package io.javalin.openapi.experimental.processor.generators

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import io.javalin.openapi.ExampleValueType
import io.javalin.openapi.NULL_STRING
import io.javalin.openapi.OpenApiExampleProperty
import java.math.BigDecimal

object ExampleGenerator {

    data class ExampleProperty(
        val name: String?,
        val value: String?,
        val type: ExampleValueType,
        val objects: List<ExampleProperty>?
    )

    fun OpenApiExampleProperty.toExampleProperty(): ExampleProperty =
        ExampleProperty(this.name, this.value, this.type, this.objects.map { it.toExampleProperty() })

    fun generateFromExamples(examples: List<ExampleProperty>): JsonElement {
        if (examples.isRawList()) {
            val jsonArray = JsonArray()
            examples.forEach { jsonArray.add(it.toJsonElement()) }
            return jsonArray
        }

        if (examples.isObjectList()) {
            val jsonArray = JsonArray()
            examples.forEach { jsonArray.add(it.toJsonElement()) }
            return jsonArray
        }

        return examples.toJsonObject()
    }

    private fun ExampleProperty.toJsonElement(): JsonElement =
        when {
            this.type == ExampleValueType.NULL -> JsonNull.INSTANCE
            this.value != NULL_STRING -> {
                when (this.type) {
                    ExampleValueType.NUMBER -> JsonPrimitive(BigDecimal(this.value))
                    ExampleValueType.BOOLEAN -> JsonPrimitive(this.value.toBoolean())
                    ExampleValueType.NULL -> JsonNull.INSTANCE
                    else -> JsonPrimitive(this.value) // STRING
                }
            }
            this.objects?.isNotEmpty() == true -> {
                // Check if objects is a list (raw or object list)
                if (objects.isRawList() || objects.isObjectList()) {
                    generateFromExamples(objects)
                } else {
                    objects.toJsonObject()
                }
            }
            else -> throw IllegalArgumentException("Example object must have either value or objects ($this)")
        }

    private fun List<ExampleProperty>.toJsonObject(): JsonObject {
        val jsonObject = JsonObject()
        this.forEach {
            if (it.name == NULL_STRING) {
                throw IllegalArgumentException("Example object must have a name ($it)")
            }
            jsonObject.add(it.name, it.toJsonElement())
        }
        return jsonObject
    }

    private fun List<ExampleProperty>.isObjectList(): Boolean =
        this.isNotEmpty() && this.all { it.name == NULL_STRING && it.value == NULL_STRING && it.objects?.isNotEmpty() ?: false }

    private fun List<ExampleProperty>.isRawList(): Boolean =
        this.isNotEmpty() && this.all { it.name == NULL_STRING && it.value != NULL_STRING && it.objects?.isEmpty() ?: true }

}