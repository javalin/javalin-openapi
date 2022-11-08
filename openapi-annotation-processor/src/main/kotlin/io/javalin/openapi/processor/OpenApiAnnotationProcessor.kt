package io.javalin.openapi.processor

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.OpenApi
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

        val openApiGenerator = OpenApiGenerator()
        openApiGenerator.generate(roundEnv)

        val jsonSchemaGenerator = JsonSchemaGenerator()
        jsonSchemaGenerator.generate(roundEnv)

        return true
    }

    override fun getSupportedAnnotationTypes(): Set<String> =
        setOf(
            OpenApi::class.qualifiedName!!,
            JsonSchema::class.qualifiedName!!
        )

    override fun getSupportedSourceVersion(): SourceVersion =
        SourceVersion.latestSupported()

}