package io.javalin.openapi.processor.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject

internal object JsonUtils {

    fun <T> toArray(list: List<T>, mapper: (T) -> String = { it.toString() }): JsonArray {
        val jsonArray = JsonArray(list.size)
        list.map(mapper).forEach { jsonArray.add(it) }
        return jsonArray
    }

    fun computeIfAbsent(root: JsonObject, key: String, value: () -> JsonObject): JsonObject {
        if (!root.has(key)) {
            root.add(key, value())
        }

        return root.getAsJsonObject(key)
    }

}