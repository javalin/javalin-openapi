package io.javalin.openapi.processor.configuration

import groovy.lang.GroovyClassLoader
import io.javalin.openapi.ExperimentalCompileOpenApiConfiguration
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiAnnotationProcessorConfigurer
import io.javalin.openapi.OpenApis
import io.javalin.openapi.processor.OpenApiAnnotationProcessor
import io.javalin.openapi.processor.OpenApiAnnotationProcessor.Companion.messager
import io.javalin.openapi.processor.shared.info
import java.io.File
import javax.annotation.processing.RoundEnvironment

class OpenApiPrecompileScriptingEngine {

    private val classLoader = OpenApiPrecompileScriptingEngine::class.java.classLoader
    private val groovyClassLoader by lazy { GroovyClassLoader(classLoader) }

    @OptIn(ExperimentalCompileOpenApiConfiguration::class)
    fun load(roundEnvironment: RoundEnvironment): OpenApiAnnotationProcessorConfigurer? =
        roundEnvironment.getElementsAnnotatedWithAny(setOf(OpenApis::class.java, OpenApi::class.java, JsonSchema::class.java))
            .firstOrNull()
            .also { messager.info("FIRST ELEMENT: $it") }
            ?.let { OpenApiAnnotationProcessor.trees.getPath(it).compilationUnit.sourceFile.name }
            ?.let {
                when {
                    /* Default sources */
                    it.contains("src".toPathSegmentIdentifier()) -> it.findSourceTargetNameBy("src") to it.substringBeforeLast("src".toPathSegmentIdentifier())
                    /* Kapt stubs */
                    it.contains("stubs".toPathSegmentIdentifier()) -> it.findSourceTargetNameBy("stubs") to it.substringBeforeLast("build".toPathSegmentIdentifier())
                    else -> null
                }
            }
            ?.let { (sourceTargetName, compileSources) -> File(compileSources).resolve("src").resolve(sourceTargetName).resolve("compile").resolve("openapi.groovy") }
            ?.also { messager.info(it.absolutePath.toString()) }
            ?.takeIf { it.exists() }
            ?.let { scriptFile -> groovyClassLoader.parseClass(scriptFile).getConstructor().newInstance() as OpenApiAnnotationProcessorConfigurer }

    private fun String.findSourceTargetNameBy(segment: String): String =
        substringAfter(segment.toPathSegmentIdentifier()).substringBefore(File.separator)

    private fun String.toPathSegmentIdentifier(): String =
        File.separator + this + File.separator

}