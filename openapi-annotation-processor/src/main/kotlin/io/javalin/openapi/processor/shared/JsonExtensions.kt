package io.javalin.openapi.processor.shared

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.javalin.openapi.NULL_STRING

internal object JsonExtensions {

    fun <T> List<T>.toJsonArray(mapper: (T) -> String = { it.toString() }): JsonArray {
        val jsonArray = JsonArray(size)
        map(mapper).forEach { jsonArray.add(it) }
        return jsonArray
    }

    fun <T> Array<T>.toJsonArray(mapper: (T) -> String = { it.toString() }): JsonArray {
        val jsonArray = JsonArray(size)
        map(mapper).forEach { jsonArray.add(it) }
        return jsonArray
    }

    fun JsonObject.computeIfAbsent(key: String, value: () -> JsonObject): JsonObject {
        if (!has(key)) {
            add(key, value())
        }

        return getAsJsonObject(key)
    }

    fun JsonObject.addString(key: String, value: String?) {
        if (NULL_STRING != value) {
            addProperty(key, value)
        }
    }

}