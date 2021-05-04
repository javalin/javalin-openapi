package net.dzikoysk.openapi.processor;

import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.javalin.plugin.openapi.annotations.HttpMethod;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.AbstractAnnotationValueVisitor8;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
import javax.tools.Diagnostic.Kind;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

@AutoService(Processor.class)
public final class OpenApiAnnotationProcessor extends AbstractProcessor {

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.messager = processingEnv.getMessager();
        messager.printMessage(Kind.WARNING, "OpenApi Annotation Processor");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
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

            for (TypeElement annotation : annotations) {
                Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

                for (Element annotatedElement : annotatedElements) {
                    AnnotationMirror openApiAnnotationMirror = annotatedElement.getAnnotationMirrors().get(0);

                    String path = getValue(openApiAnnotationMirror.getElementValues(), "path").toString();
                    HttpMethod method = HttpMethod.valueOf(getValue(openApiAnnotationMirror.getElementValues(), "method").toString().toUpperCase());

                    JsonObject pathObject = computeIfAbsent(paths, path, JsonObject::new);
                    JsonObject methodObject = computeIfAbsent(pathObject, method.name().toLowerCase(), JsonObject::new);

                    for (Entry<? extends ExecutableElement, ? extends AnnotationValue> property : openApiAnnotationMirror.getElementValues().entrySet()) {
                        methodObject.addProperty(property.getKey().getSimpleName().toString(), property.getValue().getValue().toString());
                    }
                }
            }

            messager.printMessage(Kind.WARNING, gson.toJson(openApi));
        }
        catch (Throwable throwable) {
            printException(throwable);
        }

        return true;
    }

    private void printException(Throwable throwable) {
        StringBuilder error = new StringBuilder(throwable.getClass() + ": " + throwable.getMessage());
        throwable.fillInStackTrace();

        for (StackTraceElement element : throwable.getStackTrace()) {
            error.append("  ").append(element.toString()).append(System.lineSeparator());
        }

        messager.printMessage(Kind.ERROR, error.toString());
    }

    private Object getValue(Map<? extends ExecutableElement, ? extends AnnotationValue> values, String key) {
        return values.entrySet().stream()
                .filter(element -> element.getKey().getSimpleName().contentEquals(key))
                .map(element -> element.getValue().getValue())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing '" + key + "' property in @OpenApi annotation"));
    }

    private JsonObject computeIfAbsent(JsonObject root, String key, Supplier<JsonObject> value) {
        if (!root.has(key)) {
            root.add(key, value.get());
        }

        return root.getAsJsonObject(key);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("io.javalin.plugin.openapi.annotations.OpenApi");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

}