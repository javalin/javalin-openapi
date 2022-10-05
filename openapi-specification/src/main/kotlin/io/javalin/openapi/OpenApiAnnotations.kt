/**
 * Internal copy of https://github.com/tipsy/javalin/blob/master/javalin/src/main/java/io/javalin/plugin/openapi/annotations/AnnotationApi.kt file.
 * In the future it might be replaced with a better impl.
 */

package io.javalin.openapi

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.reflect.KClass

/**
 * Provide metadata for the generation of the open api documentation to the annotated Handler.
 */
@Target(CLASS, FIELD, FUNCTION)
@Retention(SOURCE)
annotation class OpenApi(
    /** Ignore the endpoint in the open api documentation */
    val ignore: Boolean = false,
    val summary: String = NULL_STRING,
    val description: String = NULL_STRING,
    val operationId: String = NULL_STRING,
    val deprecated: Boolean = false,
    val tags: Array<String> = [],
    val cookies: Array<OpenApiParam> = [],
    val headers: Array<OpenApiParam> = [],
    val pathParams: Array<OpenApiParam> = [],
    val queryParams: Array<OpenApiParam> = [],
    val formParams: Array<OpenApiParam> = [],
    val requestBody: OpenApiRequestBody = OpenApiRequestBody([]),
    // val composedRequestBody: OpenApiComposedRequestBody = OpenApiComposedRequestBody([]), ?
    val responses: Array<OpenApiResponse> = [],
    val security: Array<OpenApiSecurity> = [],
    val path: String,
    val methods: Array<HttpMethod>
)

@Target()
annotation class OpenApiResponse(
    val status: String,
    val content: Array<OpenApiContent> = [],
    val description: String = NULL_STRING
)

@Target()
annotation class OpenApiParam(
    val name: String,
    val type: KClass<*> = String::class,
    val description: String = NULL_STRING,
    val deprecated: Boolean = false,
    val required: Boolean = false,
    val allowEmptyValue: Boolean = false,
    val isRepeatable: Boolean = false,
    val example: String = ""
)

@Target()
annotation class OpenApiRequestBody(
    val content: Array<OpenApiContent>,
    val required: Boolean = false,
    val description: String = NULL_STRING
)

//@Target()
//annotation class OpenApiComposedRequestBody(
//    val anyOf: Array<OpenApiContent> = [],
//    val oneOf: Array<OpenApiContent> = [],
//    val required: Boolean = false,
//    val description: String = NULL_STRING,
//    val contentType: String = ContentType.AUTODETECT
//)

@Target()
annotation class OpenApiContent(
    val from: KClass<*> = NULL_CLASS::class,
    val mimeType: String = ContentType.AUTODETECT,
    val type: String = NULL_STRING,
    val format: String = NULL_STRING,
    val properties: Array<OpenApiContentProperty> = []
)

@Target()
annotation class OpenApiContentProperty(
    val name: String,
    val isArray: Boolean = false,
    val type: String,
    val format: String = NULL_STRING
)

@Target()
annotation class OpenApiSecurity(
    val name: String,
    val scopes: Array<String> = []
)

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
annotation class OpenApiIgnore

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
annotation class OpenApiName(
    val value: String
)

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
annotation class OpenApiExample(
    val value: String
)

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
annotation class OpenApiPropertyType(
    val definedBy: KClass<*>
)

/** Null class because annotations do not support null values */
@Suppress("ClassName")
class NULL_CLASS

/** Null string because annotations do not support null values */
const val NULL_STRING = "-- This string represents a null value and shouldn't be used --"

object ContentType {
    const val JSON = "application/json"
    const val HTML = "text/html"
    const val FORM_DATA_URL_ENCODED = "application/x-www-form-urlencoded"
    const val FORM_DATA_MULTIPART = "multipart/form-data"
    const val AUTODETECT = "AUTODETECT - Will be replaced later"
}

enum class ComposedType {
    NULL,
    ANY_OF,
    ONE_OF;
}

enum class HttpMethod {
    POST,
    GET,
    PUT,
    PATCH,
    DELETE,
    HEAD,
    OPTIONS,
    TRACE;
}