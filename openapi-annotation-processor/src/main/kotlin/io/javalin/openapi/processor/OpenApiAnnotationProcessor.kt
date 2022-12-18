package io.javalin.openapi.processor

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sun.source.util.Trees
import groovy.lang.GroovyClassLoader
import io.javalin.openapi.ExperimentalCompileOpenApiConfiguration
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiAnnotationProcessorConfiguration
import io.javalin.openapi.OpenApiAnnotationProcessorConfigurer
import io.javalin.openapi.OpenApis
import io.javalin.openapi.processor.configuration.OpenApiPrecompileScriptingEngine
import io.javalin.openapi.processor.generators.JsonSchemaGenerator
import io.javalin.openapi.processor.generators.OpenApiGenerator
import java.io.File
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
        val gson: Gson = GsonBuilder()
            .setPrettyPrinting()
            .create()

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

        val openApiConfiguration = OpenApiAnnotationProcessorConfiguration()
        configurer?.configure(openApiConfiguration)

        if (openApiConfiguration.debug) {
            messager.printMessage(NOTE, "OpenApi | Debug mode enabled")
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