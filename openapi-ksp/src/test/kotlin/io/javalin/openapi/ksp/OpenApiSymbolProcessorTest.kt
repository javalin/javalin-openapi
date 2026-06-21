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

    @Test
    fun `writes an openapi document for an OpenApi route via the shared generator`() {
        val compilation = KotlinCompilation().apply {
            useKsp2()
            sources = listOf(
                SourceFile.kotlin(
                    "Routes.kt",
                    """
                    package fixtures
                    import io.javalin.openapi.HttpMethod
                    import io.javalin.openapi.OpenApiStatus
                    import io.javalin.openapi.OpenApi
                    import io.javalin.openapi.OpenApiContent
                    import io.javalin.openapi.OpenApiResponse

                    class Account(val id: String, val age: Int)

                    class Routes {
                        @OpenApi(
                            path = "/account",
                            methods = [HttpMethod.GET],
                            responses = [OpenApiResponse(status = OpenApiStatus.OK, content = [OpenApiContent(from = Account::class)])]
                        )
                        fun getAccount() {}
                    }
                    """.trimIndent()
                )
            )
            symbolProcessorProviders = mutableListOf(OpenApiSymbolProcessorProvider())
            inheritClassPath = true
            messageOutputStream = System.out
        }

        val result = compilation.compile()
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { "KSP compilation failed: ${result.messages}" }

        val document = compilation.kspSourcesDir.walkTopDown().firstOrNull { it.name == "openapi-default.json" }
            ?: error("openapi document not generated. Output tree:\n" + compilation.kspSourcesDir.walkTopDown().joinToString("\n"))

        val doc = jsonMapper.readTree(document.readText())
        assertThat(doc.path("openapi").asText()).isEqualTo("3.1.0")

        val operation = doc.path("paths").path("/account").path("get")
        assertThat(operation.isMissingNode).isFalse()

        assertThat(operation.path("responses").path("200").path("description").asText()).isEqualTo("OK")

        val schemaRef = operation.path("responses").path("200").path("content").path("application/json").path("schema").path("\$ref").asText()
        assertThat(schemaRef).isEqualTo("#/components/schemas/Account")

        val account = doc.path("components").path("schemas").path("Account")
        assertThat(account.path("type").asText()).isEqualTo("object")
        assertThat(account.path("properties").path("id").path("type").asText()).isEqualTo("string")
        assertThat(account.path("properties").path("age").path("type").asText()).isEqualTo("integer")
    }
}
