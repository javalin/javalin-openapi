package net.dzikoysk.openapi.processor;

import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.javalin.plugin.openapi.annotations.OpenApi;
import net.dzikoysk.openapi.processor.annotations.OpenApiLoader;
import net.dzikoysk.openapi.processor.utils.ProcessorUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

@AutoService(Processor.class)
public final class OpenApiAnnotationProcessor extends AbstractProcessor {

    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.messager = processingEnv.getMessager();
        messager.printMessage(Kind.WARNING, "OpenApi Annotation Processor");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            Collection<OpenApi> openApiAnnotations = OpenApiLoader.loadAnnotations(annotations, roundEnv);

            OpenApiGenerator generator = new OpenApiGenerator(messager);
            JsonObject result = generator.generate(openApiAnnotations);

            // TODO: result to file
        }
        catch (Throwable throwable) {
            ProcessorUtils.printException(messager, throwable);
        }

        return true;
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