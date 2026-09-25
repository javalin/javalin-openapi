package io.javalin.openapi.experimental.processor.generators

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
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

    fun generateFromExamples(examples: List<ExampleProperty>): GeneratorResult =
        GeneratorResult(simpleValue = null, jsonElement = examples.toJson())

    private fun ExampleProperty.toJson(): JsonNode =
        when {
            value != null -> TextNode.valueOf(value)
            objects?.isNotEmpty() == true -> objects.toJson()
            raw != null -> jsonMapper.readTree(raw)
            else -> throw IllegalArgumentException("Example object must have value, raw value or objects ($this)")
        }

    private fun List<ExampleProperty>.toJson(): JsonNode =
        when {
            isRawList() || isObjectList() -> createArrayNode().also { array ->
                forEach { array.add(it.toJson()) }
            }
            else -> createObjectNode().also { objectNode ->
                forEach { example ->
                    val value = example.toJson()
                    require(example.name != null) { "Example object must have a name ($example)" }
                    objectNode.set<JsonNode>(example.name, value)
                }
            }
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
