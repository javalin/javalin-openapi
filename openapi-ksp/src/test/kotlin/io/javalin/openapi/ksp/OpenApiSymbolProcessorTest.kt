package io.javalin.openapi.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.tschuchort.compiletesting.CompilationResult
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
        val (compilation, result) = compileWithKsp(
            SourceFile.kotlin(
                "Widget.kt",
                """
                package app
                import io.javalin.openapi.JsonSchema
                @JsonSchema
                class Widget(val name: String, val size: Int)
                """.trimIndent()
            )
        )
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { "KSP compilation failed: ${result.messages}" }

        val doc = compilation.generatedJson("app.Widget")
        assertThat(doc.path("type").asText()).isEqualTo("object")
        assertThat(doc.path("properties").path("name").path("type").asText()).isEqualTo("string")
        assertThat(doc.path("properties").path("size").path("type").asText()).isEqualTo("integer")
        assertThat(doc.path("properties").path("size").path("format").asText()).isEqualTo("int32")
    }

    @Test
    fun `writes an openapi document for an OpenApi route via the shared generator`() {
        val (compilation, result) = compileWithKsp(
            SourceFile.kotlin(
                "Routes.kt",
                """
                package app
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
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { "KSP compilation failed: ${result.messages}" }

        val doc = compilation.generatedJson("openapi-default.json")
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
        val (compilation, result) = compileWithKsp(
            SourceFile.kotlin(
                "Shapes.kt",
                """
                package app
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
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { "KSP compilation failed: ${result.messages}" }

        val shape = compilation.generatedJson("openapi-default.json").path("components").path("schemas").path("Shape")

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
        val (compilation, result) = compileWithKsp(
            SourceFile.kotlin(
                "Shared.kt",
                """
                package app
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
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { "KSP compilation failed: ${result.messages}" }

        val schemas = compilation.generatedJson("openapi-default.json").path("components").path("schemas")
        val addressProperty = schemas.path("Account").path("properties").path("address")

        assertThat(addressProperty.path("\$ref").asText()).isEqualTo("#/components/schemas/Address")
        assertThat(addressProperty.has("properties")).isFalse()
        assertThat(schemas.path("Address").path("properties").path("city").path("type").asText()).isEqualTo("string")
    }

    @Test
    fun `fails loudly for OpenApiByFields only true`() {
        val (_, result) = compileWithKsp(
            SourceFile.kotlin(
                "FieldsOnly.kt",
                """
                package app
                import io.javalin.openapi.JsonSchema
                import io.javalin.openapi.OpenApiByFields

                @JsonSchema
                @OpenApiByFields(only = true)
                class FieldsOnly(val value: String)
                """.trimIndent()
            )
        )

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("KSP does not support @OpenApiByFields(only = true)")
    }

    @Test
    fun `honors getter-site OpenApiIgnore annotations`() {
        val (compilation, result) = compileWithKsp(
            SourceFile.kotlin(
                "GetterIgnored.kt",
                """
                package app
                import io.javalin.openapi.JsonSchema
                import io.javalin.openapi.OpenApiIgnore

                @JsonSchema
                class GetterIgnored(@get:OpenApiIgnore val secret: String, val kept: String)
                """.trimIndent()
            )
        )
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { "KSP compilation failed: ${result.messages}" }

        val properties = compilation.generatedJson("app.GetterIgnored").path("properties")
        assertThat(properties.has("kept")).isTrue()
        assertThat(properties.has("secret")).isFalse()
    }

    @Test
    fun `does not publish private Kotlin vals as schema properties`() {
        val (compilation, result) = compileWithKsp(
            SourceFile.kotlin(
                "PrivateDto.kt",
                """
                package app
                import io.javalin.openapi.JsonSchema

                @JsonSchema
                class PrivateDto(val visible: String, private val secret: String)
                """.trimIndent()
            )
        )
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { "KSP compilation failed: ${result.messages}" }

        val properties = compilation.generatedJson("app.PrivateDto").path("properties")
        assertThat(properties.has("visible")).isTrue()
        assertThat(properties.has("secret")).isFalse()
    }

    @Test
    fun `maps ByteArray to binary string schema`() {
        val (compilation, result) = compileWithKsp(
            SourceFile.kotlin(
                "BytesDto.kt",
                """
                package app
                import io.javalin.openapi.JsonSchema

                @JsonSchema
                class BytesDto(val payload: ByteArray)
                """.trimIndent()
            )
        )
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { "KSP compilation failed: ${result.messages}" }

        val payload = compilation.generatedJson("app.BytesDto").path("properties").path("payload")
        assertThat(payload.path("type").asText()).isEqualTo("string")
        assertThat(payload.path("format").asText()).isEqualTo("binary")
    }

    @Test
    fun `deduplicates custom annotation extras across KSP property and getter sources`() {
        val (compilation, result) = compileWithKsp(
            SourceFile.kotlin(
                "DuplicateCustomAnnotationDto.kt",
                """
                package app
                import io.javalin.openapi.CustomAnnotation
                import io.javalin.openapi.JsonSchema

                @CustomAnnotation
                @Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER)
                annotation class KspExtra(val xDeduped: String)

                @JsonSchema
                class DuplicateCustomAnnotationDto(
                    @KspExtra("property")
                    @get:KspExtra("getter")
                    val value: String
                )
                """.trimIndent()
            )
        )
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { "KSP compilation failed: ${result.messages}" }

        val value = compilation.generatedJson("app.DuplicateCustomAnnotationDto")
            .path("properties")
            .path("value")

        assertThat(value.path("xDeduped").asText()).isEqualTo("property")
    }

    @Test
    fun `does not fail when another processor creates OpenApi routes in a later KSP round`() {
        val (_, result) = compileWithKsp(
            SourceFile.kotlin(
                "InitialRoutes.kt",
                """
                package app
                import io.javalin.openapi.HttpMethod
                import io.javalin.openapi.OpenApi

                class InitialRoutes {
                    @OpenApi(path = "/initial", methods = [HttpMethod.GET])
                    fun initial() {}
                }
                """.trimIndent()
            ),
            providers = mutableListOf(OpenApiSymbolProcessorProvider(), LaterRoundRouteProcessorProvider())
        )

        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result.messages).doesNotContain("FileAlreadyExistsException")
    }

    @Test
    fun `honors JsonSchema requireNonNulls false`() {
        val (compilation, result) = compileWithKsp(
            SourceFile.kotlin(
                "OptionalSchema.kt",
                """
                package app
                import io.javalin.openapi.JsonSchema

                @JsonSchema(requireNonNulls = false)
                class OptionalSchema(val name: String, val age: Int)
                """.trimIndent()
            )
        )
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { "KSP compilation failed: ${result.messages}" }

        val document = compilation.generatedJson("app.OptionalSchema")
        assertThat(document.path("\$schema").asText()).isEqualTo("https://json-schema.org/draft/2020-12/schema")
        assertThat(document.has("required")).isFalse()
        assertThat(document.path("properties").path("name").path("type").asText()).isEqualTo("string")
        assertThat(document.path("properties").path("age").path("type").asText()).isEqualTo("integer")
    }

    @Test
    fun `honors JsonSchema generateResource false`() {
        val (compilation, result) = compileWithKsp(
            SourceFile.kotlin(
                "DisabledSchema.kt",
                """
                package app
                import io.javalin.openapi.JsonSchema

                @JsonSchema(generateResource = false)
                class DisabledSchema(val ignored: String)
                """.trimIndent()
            )
        )
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { "KSP compilation failed: ${result.messages}" }

        val generatedNames = compilation.kspSourcesDir.walkTopDown().map { it.name }.toList()
        assertThat(generatedNames).doesNotContain("app.DisabledSchema")
    }

    @Test
    fun `inlines nested types in standalone JsonSchema resources`() {
        val (compilation, result) = compileWithKsp(
            SourceFile.kotlin(
                "NestedSchema.kt",
                """
                package app
                import io.javalin.openapi.JsonSchema

                class NestedChild(val value: String)

                @JsonSchema
                class NestedSchema(val child: NestedChild)
                """.trimIndent()
            )
        )
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { "KSP compilation failed: ${result.messages}" }

        val child = compilation.generatedJson("app.NestedSchema").path("properties").path("child")
        assertThat(child.path("type").asText()).isEqualTo("object")
        assertThat(child.path("properties").path("value").path("type").asText()).isEqualTo("string")
        assertThat(child.path("required").map { it.asText() }).containsExactly("value")
    }

    private fun compileWithKsp(
        vararg sources: SourceFile,
        providers: List<SymbolProcessorProvider> = listOf(OpenApiSymbolProcessorProvider()),
    ): Pair<KotlinCompilation, CompilationResult> {
        val compilation = KotlinCompilation().apply {
            useKsp2()
            this.sources = sources.toList()
            symbolProcessorProviders = providers.toMutableList()
            inheritClassPath = true
            messageOutputStream = System.out
        }

        return compilation to compilation.compile()
    }

    private fun KotlinCompilation.generatedJson(name: String) =
        jsonMapper.readTree(generatedFile(name).readText())

    private fun KotlinCompilation.generatedFile(name: String) =
        kspSourcesDir.walkTopDown().firstOrNull { it.name == name }
            ?: error("generated file $name not found. Output tree:\n" + kspSourcesDir.walkTopDown().joinToString("\n"))
}

private class LaterRoundRouteProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        LaterRoundRouteProcessor(environment.codeGenerator)
}

private class LaterRoundRouteProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (!generated) {
            generated = true
            codeGenerator.createNewFile(Dependencies(aggregating = true), "app", "GeneratedRoutes")
                .use {
                    it.write(
                        """
                        package app

                        import io.javalin.openapi.HttpMethod
                        import io.javalin.openapi.OpenApi

                        class GeneratedRoutes {
                            @OpenApi(path = "/generated", methods = [HttpMethod.GET])
                            fun generated() {}
                        }
                        """.trimIndent().toByteArray()
                    )
                }
        }

        return emptyList()
    }
}
