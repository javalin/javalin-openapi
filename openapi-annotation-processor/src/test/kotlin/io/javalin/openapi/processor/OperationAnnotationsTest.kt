@file:Suppress("unused")

package io.javalin.openapi.processor

import io.javalin.openapi.HttpMethod.POST
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiRequestBody
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.OpenApiSecurity
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test

internal class OperationAnnotationsTest : OpenApiAnnotationProcessorSpecification() {

    @OpenApi(
        path = "/parameters",
        versions = ["should_describe_parameters"],
        pathParams = [OpenApiParam(name = "id", type = Int::class, description = "Identifier", required = true)],
        queryParams = [OpenApiParam(name = "page", type = Int::class)],
        headers = [OpenApiParam(name = "X-Trace-Id")],
        cookies = [OpenApiParam(name = "session")],
    )
    @Test
    fun should_describe_parameters() = withOpenApi("should_describe_parameters") {
        assertThatJson(it)
            .inPath("$.paths['/parameters'].get.parameters")
            .isEqualTo(json("""
                [
                    { "name": "session",    "in": "cookie", "schema": { "type": "string" } },
                    { "name": "X-Trace-Id", "in": "header", "schema": { "type": "string" } },
                    { "name": "id",         "in": "path",  "description": "Identifier", "required": true, "schema": { "type": "integer", "format": "int32" } },
                    { "name": "page",       "in": "query", "schema": { "type": "integer", "format": "int32" } }
                ]
            """))
    }

    private class RequestDto(val field: String)

    @OpenApi(
        path = "/request-body",
        methods = [POST],
        versions = ["should_describe_request_body"],
        requestBody = OpenApiRequestBody(content = [OpenApiContent(from = RequestDto::class)], required = true, description = "Payload"),
    )
    @Test
    fun should_describe_request_body() = withOpenApi("should_describe_request_body") {
        assertThatJson(it)
            .inPath("$.paths['/request-body'].post.requestBody")
            .isEqualTo(json("""
                {
                    "description": "Payload",
                    "content": { "application/json": { "schema": { "${'$'}ref": "#/components/schemas/RequestDto" } } },
                    "required": true
                }
            """))

        assertThatJson(it)
            .inPath("$.components.schemas.RequestDto")
            .isEqualTo(json("""{ "type": "object", "properties": { "field": { "type": "string" } }, "required": ["field"] }"""))
    }

    @OpenApi(
        path = "/secured",
        versions = ["should_attach_security"],
        security = [OpenApiSecurity(name = "BearerAuth", scopes = ["read", "write"])],
    )
    @Test
    fun should_attach_security() = withOpenApi("should_attach_security") {
        assertThatJson(it)
            .inPath("$.paths['/secured'].get.security")
            .isEqualTo(json("""[ { "BearerAuth": ["read", "write"] } ]"""))
    }

    @OpenApi(
        path = "/responses",
        versions = ["should_describe_multiple_responses"],
        responses = [
            OpenApiResponse(status = "200", content = [OpenApiContent(from = String::class)]),
            OpenApiResponse(status = "404", description = "Not found"),
        ],
    )
    @Test
    fun should_describe_multiple_responses() = withOpenApi("should_describe_multiple_responses") {
        assertThatJson(it)
            .inPath("$.paths['/responses'].get.responses")
            .isEqualTo(json("""
                {
                    "200": { "description": "OK", "content": { "text/plain": { "schema": { "type": "string" } } } },
                    "404": { "description": "Not found" }
                }
            """))
    }

}
