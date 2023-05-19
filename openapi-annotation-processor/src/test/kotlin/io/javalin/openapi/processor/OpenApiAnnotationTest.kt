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
import org.junit.jupiter.api.Test

internal class OpenApiAnnotationTest : OpenApiAnnotationProcessorSpecification() {

    @OpenApi(
        path = "/",
        versions = ["should_generate_info"]
    )
    @Test
    fun should_generate_info() = withOpenApi("should_generate_info") {
        assertThatJson(it)
            .isObject
            .containsEntry("openapi", "3.0.3")
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
                "parameters" to json("[]"),
                "deprecated" to true,
                "security" to json("[]")
            ))
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
                                },
                                required: false
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

}