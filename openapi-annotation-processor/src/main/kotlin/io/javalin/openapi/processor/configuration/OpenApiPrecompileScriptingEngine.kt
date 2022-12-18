package io.javalin.openapi.processor.configuration

import groovy.lang.GroovyClassLoader
import io.javalin.openapi.ExperimentalCompileOpenApiConfiguration
import io.javalin.openapi.OpenApiAnnotationProcessorConfigurer
import io.javalin.openapi.OpenApis
import io.javalin.openapi.processor.OpenApiAnnotationProcessor
import java.io.File
import javax.annotation.processing.RoundEnvironment

class OpenApiPrecompileScriptingEngine {

    private val groovyClassLoader by lazy { GroovyClassLoader(this::class.java.classLoader) }

    @OptIn(ExperimentalCompileOpenApiConfiguration::class)
    fun load(roundEnvironment: RoundEnvironment): OpenApiAnnotationProcessorConfigurer? =
        roundEnvironment.getElementsAnnotatedWith(OpenApis::class.java)
            .firstOrNull()
            ?.let { OpenApiAnnotationProcessor.trees.getPath(it).compilationUnit.sourceFile.name }
            ?.let {
                val sourceDirectoryIdentifier = File.separator + "src" + File.separator
                val sourceTargetName = it.substringAfter(sourceDirectoryIdentifier).substringBefore(File.separator)
                val compileSources = it.substringBeforeLast(sourceDirectoryIdentifier)
                File(compileSources).resolve("src").resolve(sourceTargetName).resolve("compile").resolve("openapi.groovy")
            }
            ?.takeIf { it.exists() }
            ?.let { scriptFile ->
                val configurerClass = groovyClassLoader.parseClass(scriptFile)
                configurerClass.getConstructor().newInstance() as OpenApiAnnotationProcessorConfigurer
            }

}