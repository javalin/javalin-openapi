package io.javalin.openapi.processor.generators

import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApis
import io.javalin.openapi.experimental.processor.shared.saveResource
import io.javalin.openapi.processor.OpenApiAnnotationProcessor.Companion.context
import io.javalin.openapi.schema.OpenApiSchemaGenerator
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import javax.annotation.processing.RoundEnvironment
import javax.tools.Diagnostic
import javax.tools.Diagnostic.Kind.WARNING

internal class OpenApiGenerator {

    private val schemaGenerator = OpenApiSchemaGenerator(
        context = context,
        title = context.parameters.info.title,
        version = context.parameters.info.version,
    )

    fun generate(roundEnvironment: RoundEnvironment) {
        val aggregatedRoutes = roundEnvironment.getElementsAnnotatedWith(OpenApis::class.java)
            .flatMap { element -> context.annotationsOf(element).findAll(OpenApi::class.java).map { it.values } }

        val standaloneRoutes = roundEnvironment.getElementsAnnotatedWith(OpenApi::class.java)
            .flatMap { element -> context.annotationsOf(element).findAll(OpenApi::class.java).map { it.values } }

        schemaGenerator.generateVersionedSchemas(aggregatedRoutes + standaloneRoutes)
            .map { (version, generatedOpenApiSchema) ->
                val resourceName = "openapi-${version.replace(" ", "-")}.json"
                val resource = context.env.filer.saveResource(context, "openapi-plugin/$resourceName", generatedOpenApiSchema)
                    ?.toUri()
                    ?.toString()
                    ?: return

                if (context.configuration.validateWithParser) {
                    val parsedSchema = OpenAPIV3Parser().readLocation(resource, emptyList(), ParseOptions())

                    if (parsedSchema.messages.isNotEmpty()) {
                        context.env.messager.printMessage(Diagnostic.Kind.NOTE, "OpenApi Validation Warnings :: ${parsedSchema.messages.size}")
                    }

                    parsedSchema.messages.forEach {
                        context.env.messager.printMessage(WARNING, it)
                    }
                }

                resourceName
            }
            .joinToString(separator = "\n")
            .let { context.env.filer.saveResource(context, "openapi-plugin/.index", it) }
    }

}
