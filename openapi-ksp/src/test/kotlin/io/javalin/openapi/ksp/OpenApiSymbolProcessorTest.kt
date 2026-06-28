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

    @Test
    fun `auto-discovers discriminator subtypes via DiscriminatorMappingName`() {
        val compilation = KotlinCompilation().apply {
            useKsp2()
            sources = listOf(
                SourceFile.kotlin(
                    "Shapes.kt",
                    """
                    package fixtures
                    import io.javalin.openapi.Discriminator
                    import io.javalin.openapi.DiscriminatorMappingName
                    import io.javalin.openapi.DiscriminatorProperty
                    import io.javalin.openapi.HttpMethod
                    import io.javalin.openapi.OneOf
                    import io.javalin.openapi.OpenApi
                    import io.javalin.openapi.OpenApiContent
                    import io.javalin.openapi.OpenApiResponse
                    import io.javalin.openapi.OpenApiStatus

                    @OneOf(discriminator = Discriminator(property = DiscriminatorProperty(name = "type")))
                    sealed interface Shape

                    @DiscriminatorMappingName("circle")
                    data class Circle(val radius: Int) : Shape

                    @DiscriminatorMappingName("square")
                    data class Square(val side: Int) : Shape

                    class Shapes {
                        @OpenApi(
                            path = "/shape",
                            methods = [HttpMethod.GET],
                            responses = [OpenApiResponse(status = OpenApiStatus.OK, content = [OpenApiContent(from = Shape::class)])]
                        )
                        fun shape() {}
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

        val shape = jsonMapper.readTree(document.readText()).path("components").path("schemas").path("Shape")

        val refs = shape.path("oneOf").map { it.path("\$ref").asText() }
        assertThat(refs).containsExactlyInAnyOrder(
            "#/components/schemas/Circle",
            "#/components/schemas/Square",
        )

        assertThat(shape.path("discriminator").path("propertyName").asText()).isEqualTo("type")
        assertThat(shape.path("discriminator").path("mapping").path("circle").asText()).isEqualTo("#/components/schemas/Circle")
        assertThat(shape.path("discriminator").path("mapping").path("square").asText()).isEqualTo("#/components/schemas/Square")
    }

    @Test
    fun `does not leak inlined sub-schemas from the JsonSchema pass into OpenApi refs`() {
        val compilation = KotlinCompilation().apply {
            useKsp2()
            sources = listOf(
                SourceFile.kotlin(
                    "Shared.kt",
                    """
                    package fixtures
                    import io.javalin.openapi.HttpMethod
                    import io.javalin.openapi.JsonSchema
                    import io.javalin.openapi.OpenApi
                    import io.javalin.openapi.OpenApiContent
                    import io.javalin.openapi.OpenApiResponse
                    import io.javalin.openapi.OpenApiStatus

                    data class Address(val city: String)

                    @JsonSchema
                    class Profile(val address: Address)

                    data class Account(val address: Address)

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

        val schemas = jsonMapper.readTree(document.readText()).path("components").path("schemas")
        val addressProperty = schemas.path("Account").path("properties").path("address")

        assertThat(addressProperty.path("\$ref").asText()).isEqualTo("#/components/schemas/Address")
        assertThat(addressProperty.has("properties")).isFalse()
        assertThat(schemas.path("Address").path("properties").path("city").path("type").asText()).isEqualTo("string")
    }
}
