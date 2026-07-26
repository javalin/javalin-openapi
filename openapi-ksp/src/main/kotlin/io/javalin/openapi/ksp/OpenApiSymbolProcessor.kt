package io.javalin.openapi.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApis
import io.javalin.openapi.experimental.OPENAPI_INFO_TITLE
import io.javalin.openapi.experimental.OPENAPI_INFO_VERSION
import io.javalin.openapi.schema.OpenApiSchemaGenerator

class OpenApiSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {

    private val writtenResources = mutableSetOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // JsonSchema inlines references; its cache must not leak into OpenAPI component references.
        generateJsonSchemes(
            resolver = resolver,
            context = KspSchemaContext(resolver = resolver, logger = logger),
        )
        generateOpenApiDocuments(
            resolver = resolver,
            context = KspSchemaContext(resolver = resolver, logger = logger),
        )
        return emptyList()
    }

    private fun generateJsonSchemes(resolver: Resolver, context: KspSchemaContext) {
        val generated =
            resolver
                .getSymbolsWithAnnotation(JsonSchema::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()
                .mapNotNull { declaration ->
                    val type = context.introspect(declaration.asStarProjectedType())
                    val generateResource =
                        context
                            .annotationsOf(type)
                            .find(JsonSchema::class.java)
                            ?.get("generateResource")
                            ?.asBoolean()
                    if (generateResource == false) {
                        return@mapNotNull null
                    }

                    val json =
                        context
                            .typeSchemaGenerator
                            .createTypeSchema(type, inlineRefs = true)
                            .toJsonSchemaString()
                    val resourceName = declaration.qualifiedName?.asString() ?: type.fullName
                    writeResource("json-schemes/$resourceName", json)
                    resourceName
                }
                .toList()

        if (generated.isNotEmpty()) {
            writeResource("json-schemes/index", generated.joinToString(separator = "\n"))
        }
    }

    private fun generateOpenApiDocuments(resolver: Resolver, context: KspSchemaContext) {
        val routes =
            (resolver.getSymbolsWithAnnotation(OpenApi::class.qualifiedName!!) +
                resolver.getSymbolsWithAnnotation(OpenApis::class.qualifiedName!!))
                .distinct()
                .flatMap { annotated ->
                    context
                        .annotationsOf(annotated)
                        .findAll(OpenApi::class.java)
                        .map { it.values }
                }
                .toList()

        if (routes.isEmpty()) {
            return
        }

        val generator = OpenApiSchemaGenerator(
            context = context,
            title = options[OPENAPI_INFO_TITLE] ?: "",
            version = options[OPENAPI_INFO_VERSION] ?: "",
        )

        val resourceNames =
            generator
                .generateVersionedSchemas(routes)
                .map { (version, json) ->
                    val resourceName = "openapi-${version.replace(" ", "-")}.json"
                    writeResource("openapi-plugin/$resourceName", json)
                    resourceName
                }

        writeResource("openapi-plugin/.index", resourceNames.joinToString(separator = "\n"))
    }

    private fun writeResource(path: String, content: String) {
        if (!writtenResources.add(path)) {
            // KSP resources are immutable; routes another processor adds in later rounds are intentionally omitted.
            return
        }

        codeGenerator.createNewFileByPath(Dependencies(aggregating = true), path, extensionName = "")
            .use { it.write(content.toByteArray()) }
    }

}

class OpenApiSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        OpenApiSymbolProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            options = environment.options,
        )
}
