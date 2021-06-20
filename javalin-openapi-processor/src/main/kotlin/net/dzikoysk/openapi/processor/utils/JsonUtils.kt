package net.dzikoysk.openapi.processor.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.function.Supplier

object JsonUtils {

    fun <T> toArray(list: List<T>, mapper: (T) -> String = { it.toString() }): JsonArray {
        val jsonArray = JsonArray(list.size)
        list.map(mapper).forEach { jsonArray.add(it) }
        return jsonArray
    }

    fun computeIfAbsent(root: JsonObject, key: String, value: Supplier<JsonObject>): JsonObject {
        if (!root.has(key)) {
            root.add(key, value.get())
        }

        return root.getAsJsonObject(key)
    }

}