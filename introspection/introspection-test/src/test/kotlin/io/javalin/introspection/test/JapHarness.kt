package io.javalin.introspection.test

import io.javalin.introspection.jap.JapTypeIntrospector
import java.net.URI
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider

/**
 * Runs [block] inside a real annotation-processing round (in-process `javac`, classpath = the test classpath),
 * giving it a [JapTypeIntrospector] + [Elements] so JAP can be exercised exactly like the runtime backend.
 */
fun <R> withJap(block: (JapTypeIntrospector, Elements) -> R): R {
    val compiler = requireNotNull(ToolProvider.getSystemJavaCompiler()) { "A JDK is required (no system Java compiler)" }
    val fileManager = compiler.getStandardFileManager(null, null, null)
    try {
        val trigger = object : SimpleJavaFileObject(URI.create("string:///Trigger.java"), JavaFileObject.Kind.SOURCE) {
            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = "class Trigger {}"
        }
        val processor = BlockProcessor(block)
        val options = listOf("-proc:only", "-classpath", System.getProperty("java.class.path"))
        val task = compiler.getTask(null, fileManager, null, options, null, listOf(trigger))
        task.setProcessors(listOf(processor))
        check(task.call()) { "annotation processing round failed" }
        check(processor.done) { "processor did not run" }
        @Suppress("UNCHECKED_CAST")
        return processor.result as R
    } finally {
        fileManager.close()
    }
}

private class BlockProcessor<R>(private val block: (JapTypeIntrospector, Elements) -> R) : AbstractProcessor() {
    var done = false
    var result: R? = null

    override fun getSupportedAnnotationTypes(): Set<String> = setOf("*")
    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (!done) {
            result = block(JapTypeIntrospector(processingEnv.typeUtils, processingEnv.elementUtils), processingEnv.elementUtils)
            done = true
        }
        return false
    }
}
