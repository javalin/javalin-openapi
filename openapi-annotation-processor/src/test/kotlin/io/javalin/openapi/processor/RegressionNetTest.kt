@file:Suppress("unused")

package io.javalin.openapi.processor

import io.javalin.openapi.JsonSchema
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.experimental.processor.shared.jsonMapper
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Files
import javax.annotation.processing.ProcessingEnvironment
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider

internal class RegressionNetTest : OpenApiAnnotationProcessorSpecification() {

    @OpenApi(
        path = "/primitive-redirect",
        versions = ["should_keep_primitive_redirect_required"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = PrimitiveRedirectDto::class)])]
    )
    @Test
    fun should_keep_primitive_redirect_required() = withOpenApi("should_keep_primitive_redirect_required") {
        assertThatJson(it)
            .inPath("$.components.schemas.PrimitiveRedirectDto.properties.createdAt")
            .isObject
            .isEqualTo(json("""{ "type": "integer", "format": "int64" }"""))

        assertThatJson(it)
            .inPath("$.components.schemas.PrimitiveRedirectDto.required")
            .isEqualTo(json("""["createdAt"]"""))
    }

    @OpenApi(
        path = "/fluent-openapi-name",
        versions = ["should_include_fluent_accessor_with_openapi_name"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = FluentOpenApiNameDto::class)])]
    )
    @Test
    fun should_include_fluent_accessor_with_openapi_name() = withOpenApi("should_include_fluent_accessor_with_openapi_name") {
        assertThatJson(it)
            .inPath("$.components.schemas.FluentOpenApiNameDto.properties")
            .isObject
            .containsKey("age")
    }

    @OpenApi(
        path = "/inherited-custom-annotation",
        versions = ["should_include_inherited_custom_annotation_extras"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = InheritedExtraChild::class)])]
    )
    @Test
    fun should_include_inherited_custom_annotation_extras() = withOpenApi("should_include_inherited_custom_annotation_extras") {
        assertThatJson(it)
            .inPath("$.components.schemas.InheritedExtraChild")
            .isObject
            .containsEntry("inherited", "yes")
    }

    @OpenApi(
        path = "/record-extra-getter",
        versions = ["should_include_record_components_and_extra_getters"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = RecordWithExtraGetter::class)])]
    )
    @Test
    fun should_include_record_components_and_extra_getters() = withOpenApi("should_include_record_components_and_extra_getters") {
        assertThatJson(it)
            .inPath("$.components.schemas.RecordWithExtraGetter.properties")
            .isObject
            .containsKey("id")
            .containsKey("displayName")
    }

    @OpenApi(
        path = "/nested-custom-annotation",
        versions = ["should_emit_nested_custom_annotation_values_as_json"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = NestedExtraDto::class)])]
    )
    @Test
    fun should_emit_nested_custom_annotation_values_as_json() = withOpenApi("should_emit_nested_custom_annotation_values_as_json") {
        assertThatJson(it)
            .inPath("$.components.schemas.NestedExtraDto.nested")
            .isEqualTo(json("""{ "note": "x" }"""))
    }

    @JsonSchema(requireNonNulls = false)
    private class JsonSchemaWithoutRequired(val name: String, val age: Int)

    @Test
    fun should_honor_json_schema_require_non_nulls_false() = withJsonScheme("JsonSchemaWithoutRequired") {
        val document = jsonMapper.readTree(it)

        assertThat(document.path("\$schema").asText()).isEqualTo("https://json-schema.org/draft/2020-12/schema")
        assertThat(document.has("required")).isFalse()
        assertThat(document.path("properties").path("name").path("type").asText()).isEqualTo("string")
        assertThat(document.path("properties").path("age").path("type").asText()).isEqualTo("integer")
    }

    @JsonSchema(generateResource = false)
    private class DisabledJsonSchema(val ignored: String)

    @Test
    fun should_honor_json_schema_generate_resource_false() {
        val generatedNames = io.javalin.openapi.JsonSchemaLoader().loadGeneratedSchemes().map { it.name }

        assertThat(generatedNames).noneMatch { it.contains("DisabledJsonSchema") }
    }

    @JsonSchema
    private class NestedJsonSchema(val child: NestedJsonSchemaChild)

    private class NestedJsonSchemaChild(val value: String)

    @Test
    fun should_inline_nested_types_in_standalone_json_schema() = withJsonScheme("NestedJsonSchema") {
        assertThatJson(it)
            .inPath("$.properties.child")
            .isObject
            .isEqualTo(json("""
                {
                  "type": "object",
                  "properties": {
                    "value": {
                      "type": "string"
                    }
                  },
                  "required": ["value"]
                }
            """))
    }

    private class SharedNestedType(val value: String)

    private class OpenApiDocumentWithSharedNestedType(val nested: SharedNestedType)

    @JsonSchema
    private class JsonSchemaDocumentWithSharedNestedType(val nested: SharedNestedType)

    @OpenApi(
        path = "/shared-nested-type",
        versions = ["should_not_leak_openapi_references_into_json_schema"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = OpenApiDocumentWithSharedNestedType::class)])],
    )
    @Test
    fun should_not_leak_openapi_references_into_json_schema() = withJsonScheme("JsonSchemaDocumentWithSharedNestedType") {
        assertThatJson(it)
            .inPath("$.properties.nested")
            .isObject
            .doesNotContainKey("${'$'}ref")
            .containsEntry("type", "object")
            .containsEntry("properties", json("""{ "value": { "type": "string" } }"""))
    }

    @Test
    fun should_process_java_repeatable_open_api_annotations() {
        val compiler = requireNotNull(ToolProvider.getSystemJavaCompiler()) { "A JDK is required (no system Java compiler)" }
        val output = Files.createTempDirectory("openapi-repeatable")
        val source = object : SimpleJavaFileObject(URI.create("string:///app/RepeatableRoutes.java"), JavaFileObject.Kind.SOURCE) {
            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence =
                """
                package app;

                import io.javalin.openapi.HttpMethod;
                import io.javalin.openapi.OpenApi;

                class RepeatableRoutes {
                    @OpenApi(path = "/first", methods = HttpMethod.GET)
                    @OpenApi(path = "/second", methods = HttpMethod.POST)
                    public void routes() {
                    }
                }
                """.trimIndent()
        }
        val options = listOf(
            "-classpath", System.getProperty("java.class.path"),
            "-d", output.toString(),
            "-s", output.resolve("generated").toString(),
        )
        val task = compiler.getTask(null, null, null, options, null, listOf(source))
        task.setProcessors(listOf(OpenApiAnnotationProcessor()))

        assertThat(task.call()).isTrue()

        val document = Files.readString(output.resolve("openapi-plugin/openapi-default.json"))
        assertThatJson(document).inPath("$.paths").isObject
            .containsKey("/first")
            .containsKey("/second")
        assertThatJson(document).inPath("$.paths['/first']").isObject.containsKey("get")
        assertThatJson(document).inPath("$.paths['/second']").isObject.containsKey("post")
    }

    @Test
    fun should_warn_when_content_mime_type_cannot_be_resolved() {
        val compiler = requireNotNull(ToolProvider.getSystemJavaCompiler()) { "A JDK is required (no system Java compiler)" }
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        val output = Files.createTempDirectory("openapi-diagnostics")
        val source = object : SimpleJavaFileObject(URI.create("string:///app/UnresolvedContentRoute.java"), JavaFileObject.Kind.SOURCE) {
            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence =
                """
                package app;

                import io.javalin.openapi.OpenApi;
                import io.javalin.openapi.OpenApiContent;
                import io.javalin.openapi.OpenApiResponse;

                class UnresolvedContentRoute {
                    @OpenApi(
                        path = "/unresolved",
                        responses = {
                            @OpenApiResponse(status = "200", content = { @OpenApiContent() })
                        }
                    )
                    public void route() {
                    }
                }
                """.trimIndent()
        }

        val options = listOf(
            "-classpath", System.getProperty("java.class.path"),
            "-d", output.toString(),
            "-s", output.resolve("generated").toString(),
        )
        val task = compiler.getTask(null, null, diagnostics, options, null, listOf(source))
        task.setProcessors(listOf(OpenApiAnnotationProcessor()))

        assertThat(task.call()).isTrue()
        assertThat(diagnostics.diagnostics)
            .filteredOn { it.kind == Diagnostic.Kind.WARNING }
            .anySatisfy {
                assertThat(it.getMessage(null)).contains("OpenApi generator cannot find matching mime type defined")
            }
    }

    @Test
    fun should_emit_compact_debug_trace() {
        val compiler = requireNotNull(ToolProvider.getSystemJavaCompiler()) { "A JDK is required (no system Java compiler)" }
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        val output = Files.createTempDirectory("openapi-debug")
        val source = object : SimpleJavaFileObject(URI.create("string:///app/DebugRoute.java"), JavaFileObject.Kind.SOURCE) {
            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence =
                """
                package app;

                import io.javalin.openapi.OpenApi;
                import io.javalin.openapi.OpenApiContent;
                import io.javalin.openapi.OpenApiResponse;

                class DebugRoute {
                    @OpenApi(
                        path = "/debug",
                        responses = {
                            @OpenApiResponse(status = "200", content = { @OpenApiContent(from = DebugDto.class) })
                        }
                    )
                    public void route() {
                    }
                }

                class DebugDto {
                    public String getName() {
                        return "";
                    }
                }
                """.trimIndent()
        }

        val options = listOf(
            "-classpath", System.getProperty("java.class.path"),
            "-d", output.toString(),
            "-s", output.resolve("generated").toString(),
        )
        val task = compiler.getTask(null, null, diagnostics, options, null, listOf(source))
        task.setProcessors(
            listOf(
                object : OpenApiAnnotationProcessor() {
                    override fun init(processingEnv: ProcessingEnvironment) {
                        super.init(processingEnv)
                        context.configuration.debug = true
                    }
                }
            )
        )

        assertThat(task.call()).isTrue()
        val notes = diagnostics.diagnostics
            .filter { it.kind == Diagnostic.Kind.NOTE }
            .map { it.getMessage(null) }
        assertThat(notes)
            .contains(
                "OpenApi | Debug mode enabled",
                "OpenApi | Generating schema for app.DebugDto",
                "OpenApi | Resolved 1 properties for app.DebugDto: name",
            )
    }
}
