package io.javalin.openapi.processor

import io.javalin.openapi.CustomAnnotation
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiByFields
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.processor.specification.OpenApiAnnotationProcessorSpecification
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import net.javacrumbs.jsonunit.assertj.assertThatJson
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
    fun gh125() = withOpenApi("gh-125") {
        println(it)
    }

    /*
     * GH-108 Ignore inherited properties
     * ~ https://github.com/javalin/javalin-openapi/issues/108
     */

    interface SpecificRecord {
        fun getRecord(): String  // it has to be implemented
    }

    open class SpecificRecordBase {
        fun getRecordBase(): String = "RecordBase" // it'll be excluded, because annotation processor only takes declared properties from the main class
    }

    @OpenApiByFields
    class EmailRequest(val email: String) : SpecificRecordBase(), SpecificRecord {
        override fun getRecord(): String = "Record" // it will be excluded by `compile/openapi.groovy` script
    }

    @OpenApi(
        path = "gh-108",
        versions = ["gh-108"],
        responses = [OpenApiResponse(status = "200", content = [OpenApiContent(from = EmailRequest::class)])]
    )
    @Test
    fun gh108() = withOpenApi("gh-108") {
        println(it)

        assertThatJson(it)
            .inPath("$.components.schemas.EmailRequest")
            .isObject
            .containsEntry("required", json("['getEmail']"))
    }

}