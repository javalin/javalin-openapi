package io.javalin.openapi.processor

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sun.source.util.Trees
import groovy.lang.GroovyClassLoader
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiAnnotationProcessorConfigurer
import io.javalin.openapi.OpenApis
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

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (roundEnv.processingOver()) {
            return false
        }

        roundEnv.getElementsAnnotatedWith(OpenApis::class.java)
            .firstOrNull()
            ?.let { trees.getPath(it).compilationUnit.sourceFile.name }
            ?.substringBeforeLast(File.separator + "src" + File.separator)
            ?.let { File(it).resolve("src").resolve("main").resolve("resources").resolve("openapi.groovy") }
            ?.takeIf { it.exists() }
            ?.also { scriptFile ->
                val groovyClassLoader = GroovyClassLoader(this::class.java.classLoader)
                val configurerClass = groovyClassLoader.parseClass(scriptFile)
                val configurer = configurerClass.getConstructor().newInstance() as OpenApiAnnotationProcessorConfigurer
                configurer.configure()
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