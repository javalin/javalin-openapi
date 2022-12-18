package io.javalin.openapi.processor

import io.javalin.openapi.CustomAnnotation
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiAnnotationProcessorSpecification
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import org.junit.jupiter.api.Test

internal class UserCasesTest : OpenApiAnnotationProcessorSpecification() {

    /*
     * GH-125 Array fields in the custom annotations don't appear in the output
     * ~ https://github.com/javalin/javalin-openapi/issues/125
     */

    @Target(AnnotationTarget.PROPERTY_GETTER)
    @CustomAnnotation
    annotation class Schema(
        val allowableValues: Array<String> = [],
        val description: String = "",
        val example: String = "",
        val format: String = "",
        val pattern: String = ""
    )

    data class KeypairCreateResponse(
        @get:Schema(
            allowableValues = ["valid", "expired", "revoked"],
            format = "fingerprint",
            pattern = "^(valid|expired|revoked)$",
            description = "status of the key like valid|expired|revoked",
        )
        val fingerprint: String
    )

    @OpenApi(
        path = "gh-125",
        versions = ["gh-125"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = KeypairCreateResponse::class)])]
    )
    @Test
    fun gh_125() = withOpenApi("gh-125") {
        println(it)
    }

}