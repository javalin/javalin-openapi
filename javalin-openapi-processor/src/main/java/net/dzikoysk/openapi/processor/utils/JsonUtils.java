package net.dzikoysk.openapi.processor.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

public final class JsonUtils {

    private JsonUtils() {}

    public static <T> JsonArray toArray(T[] array, Function<T, String> mapper) {
        JsonArray jsonArray = new JsonArray(array.length);

        Arrays.stream(array)
                .map(mapper)
                .forEach(jsonArray::add);

        return jsonArray;
    }

    public static JsonObject computeIfAbsent(JsonObject root, String key, Supplier<JsonObject> value) {
        if (!root.has(key)) {
            root.add(key, value.get());
        }

        return root.getAsJsonObject(key);
    }

}
