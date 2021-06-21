package net.dzikoysk.openapi.processor

import net.dzikoysk.openapi.processor.annotations.OpenApiLoader
import net.dzikoysk.openapi.processor.utils.ProcessorUtils
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic.Kind.WARNING

open class OpenApiAnnotationProcessor : AbstractProcessor() {

    companion object {
        lateinit var messager: Messager
        lateinit var elements: Elements
        lateinit var types: Types
    }

    override fun init(processingEnv: ProcessingEnvironment) {
        messager = processingEnv.messager
        elements = processingEnv.elementUtils
        types = processingEnv.typeUtils
        messager.printMessage(WARNING, "OpenApi Annotation Processor")
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        try {
            val openApiAnnotations = OpenApiLoader.loadAnnotations(annotations, elements, types, roundEnv)

            val generator = OpenApiGenerator(messager)
            val result = generator.generate(openApiAnnotations)
            messager.printMessage(WARNING, result)

            // TODO: result to file
        } catch (throwable: Throwable) {
            ProcessorUtils.printException(throwable)
        }

        return true
    }

    override fun getSupportedAnnotationTypes(): Set<String> =
        setOf("io.javalin.plugin.openapi.annotations.OpenApi")

    override fun getSupportedSourceVersion(): SourceVersion =
        SourceVersion.latestSupported()

}