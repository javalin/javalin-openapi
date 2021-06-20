package net.dzikoysk.openapi.processor.utils

import net.dzikoysk.openapi.processor.OpenApiAnnotationProcessor
import javax.tools.Diagnostic.Kind.ERROR

object ProcessorUtils {

    fun printException(throwable: Throwable) {
        val messager = OpenApiAnnotationProcessor.messager
        val error = StringBuilder(throwable.javaClass.toString() + ": " + throwable.message)

        for (element in throwable.stackTrace) {
            error.append("  ").append(element.toString()).append(System.lineSeparator())
        }

        messager.printMessage(ERROR, error.toString())

        if (throwable.cause != null) {
            messager.printMessage(ERROR, "---")
            printException(throwable.cause!!)
        }
    }

}