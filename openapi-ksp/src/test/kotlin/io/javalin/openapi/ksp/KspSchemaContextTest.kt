package io.javalin.openapi.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
class KspSchemaContextTest {

    private fun <R> withResolver(block: (Resolver, KSPLogger) -> R): R {
        var result: Result<R>? = null
        val provider = object : SymbolProcessorProvider {
            override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
                object : SymbolProcessor {
                    override fun process(resolver: Resolver): List<KSAnnotated> {
                        if (result == null) result = runCatching { block(resolver, environment.logger) }
                        return emptyList()
                    }
                }
        }
        val compilation = KotlinCompilation().apply {
            useKsp2()
            sources = listOf(SourceFile.kotlin("Trigger.kt", "package trigger\nclass Trigger"))
            symbolProcessorProviders = mutableListOf(provider)
            inheritClassPath = true
            messageOutputStream = System.out
        }
        val compiled = compilation.compile()
        check(compiled.exitCode == KotlinCompilation.ExitCode.OK) { "KSP compilation failed: ${compiled.messages}" }
        return (result ?: error("KSP processor did not run")).getOrThrow()
    }

    @Test
    fun `generates a component schema end-to-end through the shared generator`() {
        withResolver { resolver, logger ->
            val context = KspSchemaContext(resolver, logger)
            val declaration = resolver.getClassDeclarationByName(
                resolver.getKSNameFromString("io.javalin.openapi.ksp.User")
            )!!
            val user = declaration
                .asStarProjectedType()

            val schema = context.componentSchema(context.introspect(user)).json
            val properties = schema.path("properties")

            assertThat(schema.path("type").asText()).isEqualTo("object")
            assertThat(properties.path("id").path("type").asText()).isEqualTo("string")
            assertThat(properties.path("age").path("type").asText()).isEqualTo("integer")
            assertThat(properties.path("age").path("format").asText()).isEqualTo("int32")
            assertThat(properties.path("tags").path("type").asText()).isEqualTo("array")
            assertThat(properties.path("tags").path("items").path("type").asText()).isEqualTo("string")
        }
    }
}

class User(
    val id: String,
    val age: Int,
    val tags: List<String>,
)
