package net.dzikoysk.openapi.processor.utils;

import net.dzikoysk.openapi.processor.OpenApiAnnotationProcessor;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic.Kind;

public final class ProcessorUtils {

    public static void printException(Throwable throwable) {
        Messager messager = OpenApiAnnotationProcessor.getMessager();
        StringBuilder error = new StringBuilder(throwable.getClass() + ": " + throwable.getMessage());

        for (StackTraceElement element : throwable.getStackTrace()) {
            error.append("  ").append(element.toString()).append(System.lineSeparator());
        }

        messager.printMessage(Kind.ERROR, error.toString());

        if (throwable.getCause() != null) {
            messager.printMessage(Kind.ERROR, "---");
            printException(throwable.getCause());
        }
    }

}
