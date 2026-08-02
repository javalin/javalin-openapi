package io.javalin.openapi.experimental.processor.shared

import io.javalin.openapi.experimental.AnnotationProcessorContext
import javax.annotation.processing.Filer
import javax.annotation.processing.FilerException
import javax.annotation.processing.Messager
import javax.tools.Diagnostic.Kind
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.NOTE
import javax.tools.FileObject
import javax.tools.StandardLocation

fun Filer.saveResource(context: AnnotationProcessorContext, name: String, content: String): FileObject? =
    try {
        val resource = createResource(StandardLocation.CLASS_OUTPUT, "", name)
        resource.openWriter().use {
            it.write(content)
        }
        resource
    } catch (_: FilerException) {
        null
    } catch (throwable: Throwable) {
        context.env.messager.printException(throwable)
        null
    }

fun Messager.info(message: String) =
    printMessage(NOTE, message)

fun Messager.printException(throwable: Throwable) {
    printException(ERROR, throwable)
}

fun Messager.printException(kind: Kind, throwable: Throwable) {
    val error = StringBuilder(throwable.javaClass.toString() + ": " + throwable.message)

    for (element in throwable.stackTrace) {
        error.append("  ").append(element.toString()).append(System.lineSeparator())
    }

    printMessage(kind, error.toString())

    throwable.cause?.let { cause ->
        printMessage(kind, "---")
        printException(cause)
    }
}
