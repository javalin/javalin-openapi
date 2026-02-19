package io.javalin.openapi.experimental.processor.shared

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.openapi.NULL_STRING

val jsonMapper: ObjectMapper = ObjectMapper()
    .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
fun createObjectNode(): ObjectNode = jsonMapper.createObjectNode()
fun createArrayNode(): ArrayNode = jsonMapper.createArrayNode()

fun Map<String, String>.toJsonObject(): ObjectNode {
    val jsonObject = createObjectNode()
    forEach { (key, value) -> jsonObject.put(key, value) }
    return jsonObject
}

fun <T> List<T>.toJsonArray(accumulator: ArrayNode.(T) -> Unit): ArrayNode {
    val jsonArray = createArrayNode()
    forEach { accumulator(jsonArray, it) }
    return jsonArray
}

fun <T> Array<T>.toJsonArray(mapper: (T) -> String = { it.toString() }): ArrayNode {
    val jsonArray = createArrayNode()
    map(mapper).forEach { jsonArray.add(it) }
    return jsonArray
}

fun ObjectNode.computeIfAbsent(key: String, value: () -> ObjectNode): ObjectNode {
    if (!has(key)) {
        set<ObjectNode>(key, value())
    }

    return get(key) as ObjectNode
}

fun ObjectNode.addString(key: String, value: String?): ObjectNode = also {
    if (NULL_STRING != value) {
        put(key, value)
    }
}

fun ObjectNode.addIfNotEmpty(key: String, value: ObjectNode): ObjectNode = also {
    if (value.size() > 0) {
        set<ObjectNode>(key, value)
    }
}

fun createJsonObjectOf(key: String, value: String): ObjectNode {
    val jsonObject = createObjectNode()
    jsonObject.put(key, value)
    return jsonObject
}
