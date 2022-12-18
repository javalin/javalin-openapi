package io.javalin.openapi.processor

import com.sun.source.util.Trees
import io.javalin.openapi.ExperimentalCompileOpenApiConfiguration
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiAnnotationProcessorConfiguration
import io.javalin.openapi.processor.configuration.OpenApiPrecompileScriptingEngine
import io.javalin.openapi.processor.generators.JsonSchemaGenerator
import io.javalin.openapi.processor.generators.OpenApiGenerator
import io.javalin.openapi.processor.shared.inDebug
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic.Kind.NOTE

open class OpenApiAnnotationProcessor : AbstractProcessor() {

    companion object {
        val configuration = OpenApiAnnotationProcessorConfiguration()

        lateinit var trees: Trees
        lateinit var messager: Messager
        lateinit var elements: Elements
        lateinit var types: Types
        lateinit var filer: Filer
    }

    override fun init(processingEnv: ProcessingEnvironment) {
        trees = Trees.instance(processingEnv)
        messager = processingEnv.messager
        elements = processingEnv.elementUtils
        types = processingEnv.typeUtils
        filer = processingEnv.filer
    }

    @OptIn(ExperimentalCompileOpenApiConfiguration::class)
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (roundEnv.processingOver()) {
            return false
        }

        val openApiPrecompileScriptingEngine = OpenApiPrecompileScriptingEngine()
        val configurer = openApiPrecompileScriptingEngine.load(roundEnv)
        configurer?.configure(configuration)

        inDebug {
            it.printMessage(NOTE, "OpenApi | Debug mode enabled")
        }

        val openApiGenerator = OpenApiGenerator()
        openApiGenerator.generate(roundEnv)

        val jsonSchemaGenerator = JsonSchemaGenerator()
        jsonSchemaGenerator.generate(roundEnv)

        return true
    }

    override fun getSupportedAnnotationTypes(): Set<String> =
        setOf(
            OpenApi::class.qualifiedName!!,
            JsonSchema::class.qualifiedName!!,
        )

    override fun getSupportedSourceVersion(): SourceVersion =
        SourceVersion.latestSupported()

}