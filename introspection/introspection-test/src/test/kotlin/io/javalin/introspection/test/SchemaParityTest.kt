package io.javalin.introspection.test

import com.fasterxml.jackson.databind.JsonNode
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import io.javalin.openapi.OpenApi
import io.javalin.openapi.dynamic.ReflectionSchemaContext
import io.javalin.openapi.experimental.processor.shared.jsonMapper
import io.javalin.openapi.ksp.OpenApiSymbolProcessorProvider
import io.javalin.openapi.processor.OpenApiAnnotationProcessor
import io.javalin.openapi.schema.OpenApiSchemaGenerator
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
@TestInstance(PER_CLASS)
class SchemaParityTest {

    private val source = SourceFile.kotlin("Models.kt", """
        package parity

        import io.javalin.openapi.*

        @JsonSchema(requireNonNulls = false)
        class Properties(
            @get:OpenApiName("display_name")
            @get:OpenApiRequired
            val name: String,
            @get:OpenApiIgnore
            val secret: String,
            @get:OpenApiNullable
            val label: String?,
            val values: List<Int>,
            val mapping: Map<String, Int>,
            val child: Child,
        )

        class Child(val id: Int)

        @OpenApi(
            path = "/users/{id}",
            pathParams = [OpenApiParam(name = "id", type = Int::class)],
            responses = [OpenApiResponse(status = "200", content = [OpenApiContent(Properties::class)])],
        )
        class Endpoint

        @JsonSchema
        enum class Role {
            @OpenApiName("administrator")
            @OpenApiDescription("Administrator")
            ADMIN,
            USER,
        }

        @JsonSchema(requireNonNulls = false)
        class FunctionGetters : Parent() {
            fun getName(): String =
                ""

            @OpenApiName("age")
            fun ageValue(): Int =
                1

            @OpenApiIgnore
            fun getIgnored(): String =
                ""

            fun issue(): String =
                ""

            fun getNeedsArgument(argument: String): String =
                argument

            fun getNothing() {}

            @OpenApiNullable
            fun getUnit(): Unit? =
                null

            suspend fun getSuspended(): String =
                ""

            private fun getPrivate(): String =
                ""

            @JvmName("getAlias")
            fun readAlias(): String =
                ""

            @JvmName("readRenamed")
            fun getRenamed(): String =
                ""
        }

        open class Parent {
            fun getInherited(): Boolean =
                true

            private fun getHidden(): String =
                ""
        }

        @JsonSchema(requireNonNulls = false)
        object StaticGetters {
            @JvmStatic
            fun getIgnored(): String =
                ""

            fun getName(): String =
                ""
        }

        class CompanionOwner {
            @JsonSchema(requireNonNulls = false)
            companion object {
                @JvmStatic
                fun getName(): String =
                    ""
            }
        }

        @JsonSchema(requireNonNulls = false)
        class RecursiveBranches(val first: Node, val second: Node)

        class Node(
            @get:OpenApiNullable
            val next: Node?,
        )
    """.trimIndent())

    private val record = SourceFile.java("AnnotatedRecord.java", """
        package parity;

        import io.javalin.openapi.*;

        @JsonSchema(requireNonNulls = false)
        public record AnnotatedRecord(
            @OpenApiIgnore
            String secret,
            @OpenApiName("display_name")
            @OpenApiRequired
            String name
        ) {
            public String getLabel() { return ""; }
            public String getName() { return name; }
            public String getSecret() { return secret; }
        }
    """.trimIndent())

    private val ap by lazy { compile(useKsp = false) }
    private val ksp by lazy { compile(useKsp = true) }

    private class CompiledSources(val resources: File, val classLoader: ClassLoader)

    private fun compile(useKsp: Boolean): CompiledSources {
        val compilation = KotlinCompilation().apply {
            sources = listOf(source, record)
            inheritClassPath = true
            jvmTarget = "17"
            when {
                useKsp -> {
                    useKsp2()
                    symbolProcessorProviders = mutableListOf(OpenApiSymbolProcessorProvider())
                }
                else -> annotationProcessors = listOf(OpenApiAnnotationProcessor())
            }
        }
        val result = compilation.compile()
        check(result.exitCode == KotlinCompilation.ExitCode.OK) { result.messages }
        val resources = when {
            useKsp -> compilation.kspSourcesDir.resolve("resources")
            else -> result.outputDirectory
        }
        return CompiledSources(resources = resources, classLoader = result.classLoader)
    }

    private fun schema(name: String): JsonNode {
        val resource = "json-schemes/parity.$name"
        val apSchema = jsonMapper.readTree(ap.resources.resolve(resource))
        val kspSchema = jsonMapper.readTree(ksp.resources.resolve(resource))
        assertThat(kspSchema).describedAs("KSP schema for %s", name).isEqualTo(apSchema)
        assertThat(runtimeSchema(name)).describedAs("Reflection schema for %s", name).isEqualTo(apSchema)
        return apSchema
    }

    private fun runtimeSchema(name: String): JsonNode {
        val context = ReflectionSchemaContext()
        return jsonMapper.readTree(
            context.typeSchemaGenerator.createTypeSchema(
                type = context.introspect(ap.classLoader.loadClass("parity.${name.replace('.', '$')}")),
                inlineRefs = true,
            ).toJsonSchemaString()
        )
    }

    @Test
    fun `route annotations and referenced components produce the same document`() {
        val resource = "openapi-plugin/openapi-default.json"
        val apDocument = jsonMapper.readTree(ap.resources.resolve(resource))
        val kspDocument = jsonMapper.readTree(ksp.resources.resolve(resource))
        val context = ReflectionSchemaContext()
        val endpoint = context.introspect(ap.classLoader.loadClass("parity.Endpoint"))
        val routes = context.annotationsOf(endpoint).findAll(OpenApi::class.java).map { it.values }
        val runtimeDocument = jsonMapper.readTree(
            OpenApiSchemaGenerator(context = context, title = "", version = "").generateSchema(routes)
        )

        assertThat(kspDocument).isEqualTo(apDocument)
        assertThat(runtimeDocument).isEqualTo(apDocument)
        val operation = apDocument.path("paths").path("/users/{id}").path("get")
        assertThat(operation.path("parameters")[0].path("required").asBoolean()).isTrue()
        val responseSchema = operation.path("responses").path("200").path("content").path("application/json").path("schema")
        assertThat(responseSchema.path($$"$ref").asText())
            .isEqualTo("#/components/schemas/Properties")
        assertThat(apDocument.path("components").path("schemas").has("Child")).isTrue()
    }

    @Test
    fun `Java record annotations and extra getters agree between AP and reflection`() {
        val apSchema = jsonMapper.readTree(ap.resources.resolve("json-schemes/parity.AnnotatedRecord"))
        assertThat(runtimeSchema("AnnotatedRecord")).isEqualTo(apSchema)
        assertThat(apSchema.path("properties").fieldNames().asSequence().toList())
            .containsExactlyInAnyOrder("display_name", "label")
        assertThat(apSchema.path("required").map { it.asText() }).containsExactly("display_name")
    }

    @Test
    fun `properties annotations and nested types produce the same schema`() {
        val schema = schema("Properties")
        val expected = jsonMapper.readTree(
            // language=json
            """
            {
              "display_name": {"type": "string"},
              "label": {"type": ["string", "null"]},
              "values": {"type": "array", "items": {"type": "integer", "format": "int32"}},
              "mapping": {"type": "object", "additionalProperties": {"type": "integer", "format": "int32"}},
              "child": {"type": "object", "properties": {"id": {"type": "integer", "format": "int32"}}}
            }
            """
        )
        assertThat(schema.path("properties")).isEqualTo(expected)
        assertThat(schema.path("required").map { it.asText() }).containsExactly("display_name")
    }

    @Test
    fun `enum names and descriptions produce the same schema`() {
        val schema = schema("Role")
        assertThat(schema.path("enum").map { it.asText() }).containsExactly("administrator", "USER")
        assertThat(schema.path("x-enum-descriptions").map { it.asText() }).containsExactly("Administrator", "")
    }

    @Test
    fun `explicit getters produce the same schema`() {
        val properties = schema("FunctionGetters").path("properties")
        assertThat(properties.fieldNames().asSequence().toList()).containsExactlyInAnyOrder("name", "age", "inherited", "private", "alias", "unit")
        assertThat(properties.path("name").path("type").asText()).isEqualTo("string")
        assertThat(properties.path("age").path("type").asText()).isEqualTo("integer")
        assertThat(properties.path("inherited").path("type").asText()).isEqualTo("boolean")
    }

    @Test
    fun `static getters are excluded by every backend`() {
        val properties = schema("StaticGetters").path("properties")
        assertThat(properties.fieldNames().asSequence().toList()).containsExactly("name")
    }

    @Test
    fun `companion getters retain their instance methods`() {
        val properties = schema("CompanionOwner.Companion").path("properties")
        assertThat(properties.fieldNames().asSequence().toList()).containsExactly("name")
    }

    @Test
    fun `recursive branches have unique reference targets`() {
        val schema = schema("RecursiveBranches")
        val anchors = schema.findValuesAsText("\$anchor")
        val references = schema.findValuesAsText("\$ref")
        assertThat(anchors).hasSize(2).doesNotHaveDuplicates()
        assertThat(references).containsExactlyInAnyOrderElementsOf(anchors.map { "#$it" })
    }
}
