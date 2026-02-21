package io.javalin.openapi.experimental.processor.shared

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

val jsonMapper: ObjectMapper = ObjectMapper()
    .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
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

fun createJsonObjectOf(key: String, value: String): ObjectNode {
    val jsonObject = createObjectNode()
    jsonObject.put(key, value)
    return jsonObject
}
