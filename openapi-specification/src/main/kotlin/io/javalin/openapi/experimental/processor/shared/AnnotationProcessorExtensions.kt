package io.javalin.openapi.experimental.processor.shared

import io.javalin.openapi.experimental.AnnotationProcessorContext
import java.io.Writer
import javax.annotation.processing.Filer
import javax.annotation.processing.FilerException
import javax.annotation.processing.Messager
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
    } catch (filerException: FilerException) {
        // file has been created during previous compilation phase
        null
    } catch (throwable: Throwable) {
        context.env.messager.printException(throwable)
        null
    }

fun Messager.info(message: String) =
    printMessage(NOTE, message)

fun Messager.printException(throwable: Throwable) {
    val error = StringBuilder(throwable.javaClass.toString() + ": " + throwable.message)

    for (element in throwable.stackTrace) {
        error.append("  ").append(element.toString()).append(System.lineSeparator())
    }

    printMessage(ERROR, error.toString())

    if (throwable.cause != null) {
        printMessage(ERROR, "---")
        printException(throwable.cause!!)
    }
}

class MessagerWriter(val context: AnnotationProcessorContext) : Writer() {

    private val builder = StringBuilder()

    override fun flush() {
        context.env.messager.info(builder.toString())
        builder.clear()
    }

    override fun write(cbuf: CharArray, off: Int, len: Int) {
        builder.append(cbuf, off, len)
    }

    override fun close() {
        flush()
    }

}