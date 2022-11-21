package io.javalin.openapi.processor.shared

import io.javalin.openapi.processor.OpenApiAnnotationProcessor
import javax.annotation.processing.FilerException
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.FileObject
import javax.tools.StandardLocation

internal object ProcessorUtils {

    fun saveResource(name: String, content: String): FileObject? =
        try {
            val resource = OpenApiAnnotationProcessor.filer.createResource(StandardLocation.CLASS_OUTPUT, "", name)
            resource.openWriter().use {
                it.write(content)
            }
            resource
        } catch (filerException: FilerException) {
            // file has been created during previous compilation phase
            null
        } catch (throwable: Throwable) {
            printException(throwable)
            null
        }

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