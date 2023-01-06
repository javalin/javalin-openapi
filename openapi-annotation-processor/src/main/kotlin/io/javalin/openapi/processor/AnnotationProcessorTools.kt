package io.javalin.openapi.processor

import com.sun.source.util.Trees
import io.javalin.openapi.experimental.processor.shared.printException
import javax.annotation.processing.ProcessingEnvironment

object AnnotationProcessorTools {

    /**
     * GH-141 Support IntelliJ's ProcessingEnvironment
     * ~ https://github.com/javalin/javalin-openapi/issues/141
     */
    fun createTrees(processingEnvironment: ProcessingEnvironment): Trees? =
        try {
            val apiWrappers = ProcessingEnvironment::class.java.classLoader.loadClass("org.jetbrains.jps.javac.APIWrappers")
            val unwrapMethod = apiWrappers.getDeclaredMethod("unwrap", Class::class.java, Object::class.java)
            val unwrapped = unwrapMethod.invoke(null, ProcessingEnvironment::class.java, processingEnvironment) as ProcessingEnvironment
            Trees.instance(unwrapped)
        } catch (ignored: Throwable) {
            processingEnvironment.messager.printException(ignored)

            try {
                Trees.instance(processingEnvironment)
            } catch (failure: Throwable) {
                processingEnvironment.messager.printException(failure)
                null
            }
        }

}