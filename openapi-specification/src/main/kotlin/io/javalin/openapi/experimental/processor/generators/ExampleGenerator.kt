package io.javalin.openapi.experimental.processor.generators

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.javalin.openapi.NULL_STRING
import io.javalin.openapi.OpenApiExampleProperty

object ExampleGenerator {

    data class GeneratorResult(val simpleValue: String?, val jsonElement: JsonElement?) {
        init {
            if (simpleValue != null && jsonElement != null) {
                throw IllegalArgumentException("rawList and jsonElement cannot be both non-null")
            }
            if (simpleValue == null && jsonElement == null) {
                throw IllegalArgumentException("rawList and jsonElement cannot be both null")
            }
        }
    }

    fun generateFromExamples(examples: Array<OpenApiExampleProperty>): GeneratorResult {
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

    private fun OpenApiExampleProperty.toSimpleExampleValue(): GeneratorResult =
        when {
            this.value != NULL_STRING -> GeneratorResult(this.value, null)
            this.objects.isNotEmpty() -> GeneratorResult(null, objects.toJsonObject())
            else -> throw IllegalArgumentException("Example object must have either value or objects ($this)")
        }

    private fun Array<OpenApiExampleProperty>.toJsonObject(): JsonObject {
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

    private fun Array<OpenApiExampleProperty>.isObjectList(): Boolean =
        this.isNotEmpty() && this.all { it.name == NULL_STRING && it.value == NULL_STRING && it.objects.isNotEmpty() }

    private fun Array<OpenApiExampleProperty>.isRawList(): Boolean =
        this.isNotEmpty() && this.all { it.name == NULL_STRING && it.value != NULL_STRING && it.objects.isEmpty() }

}