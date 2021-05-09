package net.dzikoysk.openapi.processor.utils;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic.Kind;

public final class ProcessorUtils {

    public static void printException(Messager messager, Throwable throwable) {
        StringBuilder error = new StringBuilder(throwable.getClass() + ": " + throwable.getMessage());
        throwable.fillInStackTrace();

        for (StackTraceElement element : throwable.getStackTrace()) {
            error.append("  ").append(element.toString()).append(System.lineSeparator());
        }

        messager.printMessage(Kind.ERROR, error.toString());
    }

}
