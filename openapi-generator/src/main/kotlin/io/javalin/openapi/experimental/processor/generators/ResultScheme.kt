package io.javalin.openapi.experimental.processor.generators

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.openapi.OpenApiNamingStrategy
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.processor.shared.createObjectNode
import java.math.BigDecimal

data class ResultScheme(
    val json: ObjectNode,
    val references: Set<ClassDefinition>
) {
    fun toJsonSchemaString(): String {
        val scheme = createObjectNode()
        scheme.put($$"$schema", "https://json-schema.org/draft/2020-12/schema")
        json.properties().forEach { (key, value) -> scheme.set<JsonNode>(key, value) }
        return scheme.toPrettyString()
    }
}

data class Property(
    val name: String,
    val type: ClassDefinition,
    val composition: PropertyComposition? = null,
    val required: Boolean = true,
    val nullable: Boolean = false,
    val extra: Map<String, Any?> = emptyMap()
)

fun splitCamelCase(name: String): List<String> {
    val words = mutableListOf<String>()
    val current = StringBuilder()
    for ((i, char) in name.withIndex()) {
        if (char.isUpperCase() && current.isNotEmpty()) {
            val nextIsLower = i + 1 < name.length && name[i + 1].isLowerCase()
            if (!current.last().isUpperCase() || nextIsLower) {
                words.add(current.toString())
                current.clear()
            }
        }
        current.append(char)
    }
    if (current.isNotEmpty()) {
        words.add(current.toString())
    }
    return words
}

fun translatePropertyName(strategy: OpenApiNamingStrategy, name: String): String =
    when (strategy) {
        OpenApiNamingStrategy.DEFAULT -> name
        OpenApiNamingStrategy.SNAKE_CASE -> splitCamelCase(name).joinToString("_") { it.lowercase() }
        OpenApiNamingStrategy.KEBAB_CASE -> splitCamelCase(name).joinToString("-") { it.lowercase() }
    }

fun ObjectNode.addExtra(extra: Map<String, Any?>): ObjectNode = also {
    extra
        .filterValues { it != null }
        .forEach { (key, value) ->
            when (value) {
                is JsonNode -> set<JsonNode>(key, value)
                is BigDecimal -> put(key, value)
                is Boolean -> put(key, value)
                is Int -> put(key, value)
                is Long -> put(key, value)
                is Double -> put(key, value)
                is Float -> put(key, value)
                is Short -> put(key, value.toInt())
                is Byte -> put(key, value.toInt())
                else -> put(key, value.toString())
            }
        }
}
