package net.dzikoysk.openapi.processor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.dzikoysk.openapi.processor.annotations.OpenApiInstance;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic.Kind;
import java.util.Collection;
import java.util.function.Supplier;

final class OpenApiGenerator {

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private final Messager messager;

    OpenApiGenerator(Messager messager) {
        this.messager = messager;
    }

    JsonObject generate(Collection<OpenApiInstance> annotations) {
        JsonObject openApi = new JsonObject();
        openApi.addProperty("openapi", "3.0.1");

        // somehow fill info { description, version } properties
        JsonObject info = new JsonObject();
        info.addProperty("description", "{description}");
        info.addProperty("version", "{version}");
        openApi.add("info", info);

        // initialize components
        JsonObject components = new JsonObject();
        openApi.add("components", components);

        // fill paths
        JsonObject paths = new JsonObject();
        openApi.add("paths", paths);


        for (OpenApiInstance openApiAnnotation : annotations) {
            System.out.println(openApiAnnotation.headers()[0].type());
        }

        messager.printMessage(Kind.WARNING, gson.toJson(openApi));

        return null;
    }

    private JsonObject computeIfAbsent(JsonObject root, String key, Supplier<JsonObject> value) {
        if (!root.has(key)) {
            root.add(key, value.get());
        }

        return root.getAsJsonObject(key);
    }

}
