package io.javalin.openapi.processor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Files
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider

internal class ProcessorDiagnosticsTest {

    @Test
    fun should_warn_when_content_mime_type_cannot_be_resolved() {
        val diagnostics = compile(
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
        )

        assertThat(diagnostics)
            .filteredOn { it.kind == Diagnostic.Kind.WARNING }
            .anySatisfy {
                assertThat(it.getMessage(null)).contains("OpenApi generator cannot find matching mime type defined")
            }
    }

    @Test
    fun should_emit_compact_debug_trace() {
        val diagnostics = compile(
            sourceCode =
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
                """.trimIndent(),
            processor =
                object : OpenApiAnnotationProcessor() {
                    override fun init(processingEnv: ProcessingEnvironment) {
                        super.init(processingEnv)
                        context.configuration.debug = true
                    }
                },
        )
        val notes = diagnostics
            .filter { it.kind == Diagnostic.Kind.NOTE }
            .map { it.getMessage(null) }

        assertThat(notes)
            .contains(
                "OpenApi | Debug mode enabled",
                "OpenApi | Generating schema for app.DebugDto",
                "OpenApi | Resolved 1 properties for app.DebugDto: name",
            )
    }

    @Test
    fun should_apply_custom_type_names_without_a_package() {
        val diagnostics = compile(
            sourceCode =
                """
                import io.javalin.openapi.OpenApi;
                import io.javalin.openapi.OpenApiContent;
                import io.javalin.openapi.OpenApiName;
                import io.javalin.openapi.OpenApiResponse;

                @OpenApiName("Renamed")
                class Original {
                }

                class DefaultPackageRoute {
                    @OpenApi(
                        path = "/renamed",
                        responses = {
                            @OpenApiResponse(status = "200", content = { @OpenApiContent(from = Original.class) })
                        }
                    )
                    public void route() {
                    }
                }
                """.trimIndent(),
            processor =
                object : OpenApiAnnotationProcessor() {
                    override fun init(processingEnv: ProcessingEnvironment) {
                        super.init(processingEnv)
                        context.configuration.debug = true
                    }
                },
        )

        assertThat(diagnostics.map { it.getMessage(null) })
            .contains("OpenApi | Generating schema for Renamed")
    }

    private fun compile(
        sourceCode: String,
        processor: Processor = OpenApiAnnotationProcessor(),
    ): List<Diagnostic<out JavaFileObject>> {
        val compiler = requireNotNull(ToolProvider.getSystemJavaCompiler()) { "A JDK is required (no system Java compiler)" }
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        val output = Files.createTempDirectory("openapi-processor")
        val source = object : SimpleJavaFileObject(URI.create("string:///app/TestRoute.java"), JavaFileObject.Kind.SOURCE) {
            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = sourceCode
        }
        val options = listOf(
            "-classpath", System.getProperty("java.class.path"),
            "-d", output.toString(),
            "-s", output.resolve("generated").toString(),
        )
        val task = compiler.getTask(null, null, diagnostics, options, null, listOf(source))
        task.setProcessors(listOf(processor))

        assertThat(task.call()).isTrue()
        return diagnostics.diagnostics
    }

}
