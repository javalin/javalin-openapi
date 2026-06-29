package io.javalin.introspection.test

import io.javalin.introspection.ClassDefinition
import io.javalin.introspection.jap.JapTypeIntrospector
import java.net.URI
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider
import kotlin.reflect.KClass

/**
 * Runs the annotation-processing backend inside a real in-process `javac` run, so it can be tested like reflection.
 *
 * The backend reads types via `javax.lang.model`, which the compiler only exposes to a running processor - there is
 * no such environment at plain runtime. So [introspect] compiles a throwaway source with a processor attached,
 * resolves [type] from inside it, and passes the result to [block]. The compiler discards its model once the run
 * ends, so [block] must finish all introspection (e.g. [ClassDefinition.getProperties]) before returning.
 */
object AnnotationProcessing {

    fun <R> introspect(type: KClass<*>, block: (ClassDefinition) -> R): R {
        val compiler = requireNotNull(ToolProvider.getSystemJavaCompiler()) { "A JDK is required (no system Java compiler)" }
        val trigger = object : SimpleJavaFileObject(URI.create("string:///Trigger.java"), JavaFileObject.Kind.SOURCE) {
            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = "class Trigger {}"
        }

        var result: Result<R>? = null
        val processor = object : AbstractProcessor() {
            override fun getSupportedAnnotationTypes(): Set<String> = setOf("*")
            override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()
            override fun process(annotations: Set<TypeElement>, round: RoundEnvironment): Boolean {
                if (result == null) {
                    val backend = JapTypeIntrospector(processingEnv.typeUtils, processingEnv.elementUtils)
                    val mirror = processingEnv.elementUtils.getTypeElement(type.qualifiedName).asType()
                    result = runCatching { block(backend.introspect(mirror)) }
                }
                return false
            }
        }

        val options = listOf("-proc:only", "-classpath", System.getProperty("java.class.path"))
        val task = compiler.getTask(null, null, null, options, null, listOf(trigger))
        task.setProcessors(listOf(processor))
        check(task.call()) { "annotation processing run failed" }
        return (result ?: error("processor did not run")).getOrThrow()
    }
}
