package io.javalin.openapi.processor.configuration

import groovy.lang.GroovyClassLoader
import io.javalin.openapi.experimental.ExperimentalCompileOpenApiConfiguration
import io.javalin.openapi.experimental.OPENAPI_GROOVY_SCRIPT_PATH
import io.javalin.openapi.experimental.OpenApiAnnotationProcessorConfigurer
import io.javalin.openapi.experimental.processor.shared.info
import io.javalin.openapi.processor.OpenApiAnnotationProcessor.Companion.context
import java.io.File

class OpenApiPrecompileScriptingEngine {

    private val classLoader = OpenApiPrecompileScriptingEngine::class.java.classLoader
    private val groovyClassLoader by lazy { GroovyClassLoader(classLoader) }

    @OptIn(ExperimentalCompileOpenApiConfiguration::class)
    fun load(): OpenApiAnnotationProcessorConfigurer? {
        val path = context.env.options[OPENAPI_GROOVY_SCRIPT_PATH] ?: return null
        val scriptFile = File(path)

        context.env.messager.info(scriptFile.absolutePath)

        if (!scriptFile.exists()) {
            throw IllegalArgumentException("OpenAPI groovy script not found at $path")
        }

        return groovyClassLoader.parseClass(scriptFile).getConstructor().newInstance() as OpenApiAnnotationProcessorConfigurer
    }

}
