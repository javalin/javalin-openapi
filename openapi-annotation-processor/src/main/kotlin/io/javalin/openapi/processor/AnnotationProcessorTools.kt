package io.javalin.openapi.processor

import com.sun.source.util.Trees
import io.javalin.openapi.experimental.processor.shared.printException
import java.lang.reflect.Proxy
import javax.annotation.processing.ProcessingEnvironment
import javax.tools.Diagnostic.Kind.NOTE


object AnnotationProcessorTools {

    /**
     * GH-141 Support IntelliJ's ProcessingEnvironment
     * ~ https://github.com/javalin/javalin-openapi/issues/141
     */
    fun createTrees(processingEnvironment: ProcessingEnvironment): Trees? =
        runCatching {
            unwrap(processingEnvironment)
                ?.let { Trees.instance(it) }
                ?: Trees.instance(processingEnvironment)
        }.getOrNull()

    private fun unwrap(processingEnv: ProcessingEnvironment): ProcessingEnvironment? =
        when {
            Proxy.isProxyClass(processingEnv.javaClass) -> {
                val invocationHandler = Proxy.getInvocationHandler(processingEnv)

                try {
                    val field = invocationHandler.javaClass.getDeclaredField("val\$delegateTo")
                    field.isAccessible = true

                    when (val delegateTo = field.get(invocationHandler)) {
                        is ProcessingEnvironment -> delegateTo
                        else -> {
                            processingEnv.messager.printMessage(NOTE, "got ${delegateTo.javaClass} expected instanceof com.sun.tools.javac.processing.JavacProcessingEnvironment")
                            null
                        }
                    }
                } catch (exception: Exception) {
                    processingEnv.messager.printException(NOTE, exception)
                    null
                }
            }
            else -> processingEnv
        }

}