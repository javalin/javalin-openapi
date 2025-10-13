package io.javalin.openapi.experimental.processor.generators

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.javalin.openapi.NULL_STRING
import io.javalin.openapi.OpenApiExampleProperty

object ExampleGenerator {

    data class ExampleProperty(
        val name: String?,
        val value: String?,
        val objects: List<ExampleProperty>?
    )

    fun OpenApiExampleProperty.toExampleProperty(): ExampleProperty =
        ExampleProperty(this.name, this.value, this.objects.map { it.toExampleProperty() })

    data class GeneratorResult(val simpleValue: String?, val jsonElement: JsonElement?) {
        init {
            when {
                simpleValue != null && jsonElement != null -> throw IllegalArgumentException("rawList and jsonElement cannot be both non-null")
                simpleValue == null && jsonElement == null -> throw IllegalArgumentException("rawList and jsonElement cannot be both null")
            }
        }
    }

    fun generateFromExamples(examples: List<ExampleProperty>): GeneratorResult {
        if (examples.isRawList()) {
            val jsonArray = JsonArray()
            examples.forEach { jsonArray.add(it.value) }
            return GeneratorResult(null, jsonArray)
        }

        if (examples.isObjectList()) {
            val jsonArray = JsonArray()
            examples.forEach { jsonArray.add(it.toSimpleExampleValue().jsonElement!!) }
            return GeneratorResult(null, jsonArray)
        }

        return GeneratorResult(null, examples.toJsonObject())
    }

    private fun ExampleProperty.toSimpleExampleValue(): GeneratorResult =
        when {
            this.value != NULL_STRING -> GeneratorResult(this.value, null)
            this.objects?.isNotEmpty() == true -> {
                // Check if objects is a list (raw or object list)
                if (objects.isRawList() || objects.isObjectList()) {
                    generateFromExamples(objects)
                } else {
                    GeneratorResult(null, objects.toJsonObject())
                }
            }
            else -> throw IllegalArgumentException("Example object must have either value or objects ($this)")
        }

    private fun List<ExampleProperty>.toJsonObject(): JsonObject {
        val jsonObject = JsonObject()
        this.forEach {
            val result = it.toSimpleExampleValue()
            if (it.name == NULL_STRING) {
                throw IllegalArgumentException("Example object must have a name ($it)")
            }
            when {
                result.simpleValue != null -> jsonObject.addProperty(it.name, result.simpleValue)
                result.jsonElement != null -> jsonObject.add(it.name, result.jsonElement)
            }
        }
        return jsonObject
    }

    private fun List<ExampleProperty>.isObjectList(): Boolean =
        this.isNotEmpty() && this.all { it.name == NULL_STRING && it.value == NULL_STRING && it.objects?.isNotEmpty() ?: false }

    private fun List<ExampleProperty>.isRawList(): Boolean =
        this.isNotEmpty() && this.all { it.name == NULL_STRING && it.value != NULL_STRING && it.objects?.isEmpty() ?: true }

}