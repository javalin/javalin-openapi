@file:Suppress("unused")

package io.javalin.openapi.processor

import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiCallback
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiRequestBody
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Files
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider

internal class OpenApiAnnotationTest : OpenApiAnnotationProcessorSpecification() {

    @OpenApi(
        path = "/",
        versions = ["should_generate_info"]
    )
    @Test
    fun should_generate_info() = withOpenApi("should_generate_info") {
        assertThatJson(it)
            .isObject
            .containsEntry("openapi", "3.1.0")
            .containsEntry("info", json("""{ "title":"", "version": "" }"""))
    }

    @OpenApi(
        path = "/basic",
        versions = ["should_contain_all_basic_properties_from_openapi_annotation"],
        summary = "Test summary",
        operationId = "Test operation id",
        description = "Test description",
        tags = ["Test tag"],
        deprecated = true,
    )
    @Test
    fun should_contain_all_basic_properties_from_openapi_annotation() = withOpenApi("should_contain_all_basic_properties_from_openapi_annotation") {
        assertThatJson(it)
            .inPath("$.paths['/basic'].get")
            .isObject
            .containsAllEntriesOf(linkedMapOf(
                "tags" to json("['Test tag']"),
                "summary" to "Test summary",
                "description" to "Test description",
                "operationId" to "Test operation id",
                "deprecated" to true,
            ))

        assertThatJson(it)
            .inPath("$.paths['/basic'].get")
            .isObject
            .doesNotContainKey("parameters")
            .doesNotContainKey("security")
    }

    @OpenApi(
        path = "/callback",
        versions = ["should_generate_callback"],
        callbacks = [
            OpenApiCallback(
                name = "onData",
                url = "{${'$'}request.body#/url}/callback",
                method = HttpMethod.POST,
                summary = "Test summary",
                description = "Test description",
                requestBody = OpenApiRequestBody(
                    content = [OpenApiContent(from = String::class)]
                ),
                responses = [
                    OpenApiResponse(status = "200", content = [OpenApiContent(from = String::class)])
                ]
            )
        ]
    )
    @Test
    fun should_generate_callback() = withOpenApi("should_generate_callback") {
        println(it)

        assertThatJson(it)
            .inPath("$.paths['/callback'].get.callbacks")
            .isObject
            .isEqualTo(json("""{
                "onData": {
                    "{${'$'}request.body#/url}/callback": {
                        "post": {
                            "summary": "Test summary",
                            "description": "Test description",
                            "requestBody": {
                                "content": {
                                    "text/plain": {
                                        "schema": {
                                            "type": "string"
                                        }
                                    }
                                }
                            },
                            "responses": {
                                "200": {
                                    "description": "OK",
                                    "content": {
                                        "text/plain": {
                                            "schema": {
                                                "type": "string"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }"""))
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

}
