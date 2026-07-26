package io.javalin.openapi.experimental.processor.generators

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.openapi.NULL_STRING
import io.javalin.openapi.experimental.processor.shared.createArrayNode
import io.javalin.openapi.experimental.processor.shared.createObjectNode
import io.javalin.openapi.experimental.processor.shared.jsonMapper

data class ExampleProperty(
    val name: String?,
    val value: String?,
    val raw: String?,
    val objects: List<ExampleProperty>?,
)

fun Map<String, Any?>.toExampleProperty(): ExampleProperty =
    ExampleProperty(
        name = (get("name") as? String)?.takeIf { it != NULL_STRING },
        value = (get("value") as? String)?.takeIf { it != NULL_STRING },
        raw = (get("raw") as? String)?.takeIf { it != NULL_STRING },
        objects = (get("objects") as? List<*>)
            ?.filterIsInstance<Map<String, Any?>>()
            ?.map { it.toExampleProperty() }
            ?.takeIf { it.isNotEmpty() },
    )

object ExampleGenerator {

    data class GeneratorResult(
        val simpleValue: String?,
        val jsonElement: JsonNode?,
    ) {
        init {
            when {
                simpleValue != null && jsonElement != null -> throw IllegalArgumentException("simpleValue and jsonElement cannot be both non-null")
                simpleValue == null && jsonElement == null -> throw IllegalArgumentException("simpleValue and jsonElement cannot be both null")
            }
        }
    }

    fun generateFromExamples(examples: List<ExampleProperty>): GeneratorResult {
        if (examples.isRawList()) {
            val jsonArray = createArrayNode()
            examples.forEach { jsonArray.add(it.value) }
            return GeneratorResult(null, jsonArray)
        }

        if (examples.isObjectList()) {
            val jsonArray = createArrayNode()
            examples.forEach { jsonArray.add(it.toSimpleExampleValue().jsonElement!!) }
            return GeneratorResult(null, jsonArray)
        }

        return GeneratorResult(null, examples.toJsonObject())
    }

    private fun ExampleProperty.toSimpleExampleValue(): GeneratorResult =
        when {
            value != null -> GeneratorResult(value, null)
            objects?.isNotEmpty() == true -> generateFromExamples(objects)
            raw != null -> GeneratorResult(null, jsonMapper.readTree(raw))
            else -> throw IllegalArgumentException("Example object must have value, raw value or objects ($this)")
        }

    private fun List<ExampleProperty>.toJsonObject(): ObjectNode {
        val jsonObject = createObjectNode()
        forEach {
            val result = it.toSimpleExampleValue()
            if (it.name == null) {
                throw IllegalArgumentException("Example object must have a name ($it)")
            }
            when {
                result.simpleValue != null -> jsonObject.put(it.name, result.simpleValue)
                result.jsonElement != null -> jsonObject.set<JsonNode>(it.name, result.jsonElement)
            }
        }
        return jsonObject
    }

    private fun List<ExampleProperty>.isObjectList(): Boolean =
        isNotEmpty() && all { example ->
            example.name == null &&
                example.value == null &&
                example.objects?.isNotEmpty() == true
        }

    private fun List<ExampleProperty>.isRawList(): Boolean =
        isNotEmpty() && all { example ->
            example.name == null &&
                example.value != null &&
                example.objects.isNullOrEmpty()
        }

}
