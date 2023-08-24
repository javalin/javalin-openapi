package io.javalin.openapi.processor

import com.sun.source.util.Trees
import io.javalin.openapi.experimental.ExperimentalCompileOpenApiConfiguration
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.OpenApi
import io.javalin.openapi.experimental.AnnotationProcessorContext
import io.javalin.openapi.experimental.OPENAPI_INFO_TITLE
import io.javalin.openapi.experimental.OPENAPI_INFO_VERSION
import io.javalin.openapi.experimental.OpenApiAnnotationProcessorConfiguration
import io.javalin.openapi.experimental.OpenApiAnnotationProcessorParameters
import io.javalin.openapi.experimental.OpenApiAnnotationProcessorParameters.Info
import io.javalin.openapi.processor.configuration.OpenApiPrecompileScriptingEngine
import io.javalin.openapi.processor.generators.JsonSchemaGenerator
import io.javalin.openapi.processor.generators.OpenApiGenerator
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind.NOTE

open class OpenApiAnnotationProcessor : AbstractProcessor() {

    companion object {
        internal lateinit var context: AnnotationProcessorContext
    }

    override fun init(processingEnv: ProcessingEnvironment) {
        context = AnnotationProcessorContext(
            parameters = OpenApiAnnotationProcessorParameters(
                info = Info(
                    title = processingEnv.options[OPENAPI_INFO_TITLE] ?: "",
                    version = processingEnv.options[OPENAPI_INFO_VERSION] ?: ""
                )
            ),
            configuration = OpenApiAnnotationProcessorConfiguration(),
            env = processingEnv,
            trees = AnnotationProcessorTools.createTrees(processingEnv)
        )
    }

    @OptIn(ExperimentalCompileOpenApiConfiguration::class)
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (roundEnv.processingOver()) {
            return false
        }
        context.roundEnv = roundEnv

        val openApiPrecompileScriptingEngine = OpenApiPrecompileScriptingEngine()
        val configurer = openApiPrecompileScriptingEngine.load(roundEnv)
        configurer?.configure(context.configuration)

        context.inDebug {
            it.printMessage(NOTE, "OpenApi | Debug mode enabled")
        }

        val openApiGenerator = OpenApiGenerator()
        openApiGenerator.generate(roundEnv)

        val jsonSchemaGenerator = JsonSchemaGenerator()
        jsonSchemaGenerator.generate(roundEnv)

        return true
    }

    override fun getSupportedOptions(): Set<String> =
        setOf(
            OPENAPI_INFO_TITLE,
            OPENAPI_INFO_VERSION,
        )

    override fun getSupportedAnnotationTypes(): Set<String> =
        setOf(
            OpenApi::class.qualifiedName!!,
            JsonSchema::class.qualifiedName!!,
        )

    override fun getSupportedSourceVersion(): SourceVersion =
        SourceVersion.latestSupported()

}