package io.javalin.openapi.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.javalin.openapi.JsonSchema

class OpenApiSymbolProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val context = KspSchemaContext(resolver)

        val generated = resolver.getSymbolsWithAnnotation(JsonSchema::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { declaration ->
                val type = context.introspect(declaration.asStarProjectedType())
                if (context.annotationsOf(type).memberValues(JsonSchema::class.java)?.get("generateResource") == false) {
                    return@mapNotNull null
                }
                val json = context.typeSchemaGenerator.createTypeSchema(type, inlineRefs = true).toJsonSchemaString()
                writeResource("json-schemes/${type.fullName}", json)
                type.fullName
            }
            .toList()

        if (generated.isNotEmpty()) {
            writeResource("json-schemes/index", generated.joinToString(separator = "\n"))
        }

        return emptyList()
    }

    private fun writeResource(path: String, content: String) =
        codeGenerator.createNewFileByPath(Dependencies(aggregating = true), path, extensionName = "")
            .use { it.write(content.toByteArray()) }

}

class OpenApiSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        OpenApiSymbolProcessor(environment.codeGenerator)
}
