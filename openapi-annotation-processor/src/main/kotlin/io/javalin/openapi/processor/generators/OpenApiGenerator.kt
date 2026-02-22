package io.javalin.openapi.processor.generators

import io.javalin.http.HttpStatus
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
        defaultStatusDescription = { status ->
            status.toIntOrNull()?.let { HttpStatus.forStatus(it) }?.message
        }
    )

    fun generate(roundEnvironment: RoundEnvironment) {
        val aggregatedOpenApiAnnotations = roundEnvironment.getElementsAnnotatedWith(OpenApis::class.java)
            .flatMap { element ->
                element
                    .getAnnotation(OpenApis::class.java)!!
                    .value
                    .asSequence()
                    .map { element to it }
            }

        val standaloneOpenApiAnnotations =
            roundEnvironment
                .getElementsAnnotatedWith(OpenApi::class.java)
                .map { it to it.getAnnotation(OpenApi::class.java)!! }

        val openApiAnnotationsByVersion = (aggregatedOpenApiAnnotations + standaloneOpenApiAnnotations)
            .flatMap { it.second.versions.map { version -> version to it } }
            .groupBy { (version, _) -> version }
            .mapValues { (_, annotations) -> annotations.map { it.second } }

        openApiAnnotationsByVersion
            .map { (version, openApiAnnotations) ->
                val preparedOpenApiAnnotations = openApiAnnotations.toSet()
                val generatedOpenApiSchema = schemaGenerator.generateSchema(preparedOpenApiAnnotations)

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
