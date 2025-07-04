@file:Suppress("unused")

package io.javalin.openapi.processor

import io.javalin.openapi.*
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
        fun getRecordBase(): String = "RecordBase" // it'll be excluded
    }

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
        assertThatJson(it)
            .inPath("$.components.schemas.EmailRequest")
            .isObject
            .containsEntry("required", json("['email']"))
    }

    /*
     * GH-151 Support auto-generated operationId like in old OpenApi plugin
     * ~ https://github.com/javalin/javalin-openapi/issues/151
     */

    @OpenApi(
        path = "/api/panda/list",
        operationId = OpenApiOperation.AUTO_GENERATE,
        versions = ["should_generate_operation_id_from_path"]
    )
    @Test
    fun should_generate_operation_id_from_path() = withOpenApi("should_generate_operation_id_from_path") {
        println(it)

        assertThatJson(it)
            .inPath("$.paths['/api/panda/list'].get.operationId")
            .isString
            .isEqualTo("getApiPandaList")
    }

    @OpenApi(
        path = "/api/panda/{pandaId}/name/<startsWith>",
        operationId = OpenApiOperation.AUTO_GENERATE,
        versions = ["should_generate_operation_id_from_path_with_parameters"]
    )
    @Test
    fun should_generate_operation_id_from_path_with_parameters() = withOpenApi("should_generate_operation_id_from_path_with_parameters"){
        println(it)

        assertThatJson(it)
            .inPath("$.paths['/api/panda/{pandaId}/name/<startsWith>'].get.operationId")
            .isString
            .isEqualTo("getApiPandaByPandaIdNameByStartsWith")
    }

    @OpenApi(
        path = "/api/cat/{cat-id}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        versions = ["should_generate_operation_id_from_path_with_parameters_hyphenated"]
    )
    @Test
    fun should_generate_operation_id_from_path_with_parameters_hyphenated() = withOpenApi("should_generate_operation_id_from_path_with_parameters_hyphenated"){
        println(it)
        // TODO not sure what to expect here
        assertThatJson(it)
            .inPath("$.paths['/api/cat/{cat-id}'].get.operationId")
            .isString
            .isEqualTo("getApiCatByCatId")
    }

    @OpenApi(
        path = "/api/panda",
        methods= [HttpMethod.PUT],
        operationId = OpenApiOperation.AUTO_GENERATE,
        versions = ["should_generate_operation_id_from_path_method_put"]
    )
    @Test
    fun should_generate_operation_id_from_path_method_put() = withOpenApi("should_generate_operation_id_from_path_method_put") {
        println(it)

        assertThatJson(it)
            .inPath("$.paths['/api/panda'].put.operationId")
            .isString
            .isEqualTo("putApiPanda")
    }

    @OpenApi(
        path = "/vip-accounts/{vip-account-id}",
        operationId = OpenApiOperation.AUTO_GENERATE,
        versions = ["should_generate_operation_id_from_hyphenated_path_with_parameters_hyphenated"]
    )
    @Test
    fun should_generate_operation_id_from_hyphenated_path_with_parameters_hyphenated() = withOpenApi("should_generate_operation_id_from_hyphenated_path_with_parameters_hyphenated"){
        println(it)

        assertThatJson(it)
            .inPath("$.paths['/vip-accounts/{vip-account-id}'].get.operationId")
            .isString
            .isEqualTo("getVipAccountsByVipAccountId")
    }

}
