package net.dzikoysk.openapi.processor;

import com.google.auto.service.AutoService;
import com.google.gson.JsonObject;
import net.dzikoysk.openapi.processor.annotations.OpenApiInstance;
import net.dzikoysk.openapi.processor.annotations.OpenApiLoader;
import net.dzikoysk.openapi.processor.utils.ProcessorUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

@AutoService(Processor.class)
public final class OpenApiAnnotationProcessor extends AbstractProcessor {

    private static Messager messager;
    private static Elements elements;
    private static Types types;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        messager = processingEnv.getMessager();
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        messager.printMessage(Kind.WARNING, "OpenApi Annotation Processor");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            Collection<OpenApiInstance> openApiAnnotations = OpenApiLoader.loadAnnotations(annotations, elements, types, roundEnv);

            OpenApiGenerator generator = new OpenApiGenerator(messager);
            JsonObject result = generator.generate(openApiAnnotations);

            // TODO: result to file
        }
        catch (Throwable throwable) {
            ProcessorUtils.printException(throwable);
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

    public static Types getTypes() {
        return types;
    }

    public static Messager getMessager() {
        return messager;
    }

}