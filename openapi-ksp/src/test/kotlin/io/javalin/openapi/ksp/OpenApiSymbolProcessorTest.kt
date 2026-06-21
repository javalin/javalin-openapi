package io.javalin.openapi.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import io.javalin.openapi.experimental.processor.shared.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
class OpenApiSymbolProcessorTest {

    @Test
    fun `writes a json-scheme resource for a JsonSchema type via the shared generator`() {
        val compilation = KotlinCompilation().apply {
            useKsp2()
            sources = listOf(
                SourceFile.kotlin(
                    "Widget.kt",
                    """
                    package fixtures
                    import io.javalin.openapi.JsonSchema
                    @JsonSchema
                    class Widget(val name: String, val size: Int)
                    """.trimIndent()
                )
            )
            symbolProcessorProviders = mutableListOf(OpenApiSymbolProcessorProvider())
            inheritClassPath = true
            messageOutputStream = System.out
        }

        val result = compilation.compile()
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { "KSP compilation failed: ${result.messages}" }

        val scheme = compilation.kspSourcesDir.walkTopDown().firstOrNull { it.name == "fixtures.Widget" }
            ?: error("json-scheme not generated. Output tree:\n" + compilation.kspSourcesDir.walkTopDown().joinToString("\n"))

        val doc = jsonMapper.readTree(scheme.readText())
        assertThat(doc.path("type").asText()).isEqualTo("object")
        assertThat(doc.path("properties").path("name").path("type").asText()).isEqualTo("string")
        assertThat(doc.path("properties").path("size").path("type").asText()).isEqualTo("integer")
        assertThat(doc.path("properties").path("size").path("format").asText()).isEqualTo("int32")
    }
}
