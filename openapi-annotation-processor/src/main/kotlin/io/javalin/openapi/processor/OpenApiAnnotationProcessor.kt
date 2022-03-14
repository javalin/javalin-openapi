package io.javalin.openapi.processor

import io.javalin.openapi.processor.annotations.OpenApiLoader
import io.javalin.openapi.processor.utils.ProcessorUtils
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import javax.tools.Diagnostic.Kind.WARNING
import javax.tools.StandardLocation

open class OpenApiAnnotationProcessor : AbstractProcessor() {

    companion object {
        lateinit var messager: Messager
        lateinit var elements: Elements
        lateinit var types: Types
        lateinit var filer: Filer
    }

    override fun init(processingEnv: ProcessingEnvironment) {
        messager = processingEnv.messager
        elements = processingEnv.elementUtils
        types = processingEnv.typeUtils
        filer = processingEnv.filer
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (roundEnv.processingOver()) {
            return false
        }

        // messager.printMessage(Diagnostic.Kind.NOTE, "OpenApi Annotation Processor :: ${annotations.size} annotation(s) found")

        try {
            val openApiAnnotations = OpenApiLoader.loadAnnotations(annotations, roundEnv)
            val generator = OpenApiGenerator()
            val result = generator.generate(openApiAnnotations)

            val resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "openapi.json")
            val location = resource.toUri()

            resource.openWriter().use {
                it.write(result)
            }

            val parsedSchema = OpenAPIV3Parser().readLocation(location.toString(), emptyList(), ParseOptions())

            if (parsedSchema.messages.size > 0) {
                messager.printMessage(Diagnostic.Kind.NOTE, "OpenApi Validation Warnings :: ${parsedSchema.messages.size}")
            }

            parsedSchema.messages.forEach {
                messager.printMessage(WARNING, it)
            }
        } catch (throwable: Throwable) {
            ProcessorUtils.printException(throwable)
        }

        return true
    }

    override fun getSupportedAnnotationTypes(): Set<String> =
        setOf("io.javalin.openapi.OpenApi")

    override fun getSupportedSourceVersion(): SourceVersion =
        SourceVersion.latestSupported()

}