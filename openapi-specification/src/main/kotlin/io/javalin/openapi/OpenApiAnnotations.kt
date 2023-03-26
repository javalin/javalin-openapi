/**
 * Internal copy of https://github.com/tipsy/javalin/blob/master/javalin/src/main/java/io/javalin/plugin/openapi/annotations/AnnotationApi.kt file.
 * In the future it might be replaced with a better impl.
 */

package io.javalin.openapi

import io.javalin.openapi.HttpMethod.GET
import io.javalin.openapi.Visibility.PUBLIC
import java.lang.annotation.Repeatable
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.reflect.KClass

/**
 * Provide metadata for the generation of the open api documentation to the annotated Handler.
 * Source: [Specification](https://swagger.io/specification/)
 */
@Repeatable(value = OpenApis::class)
@Target(CLASS, FIELD, FUNCTION)
@Retention(RUNTIME)
annotation class OpenApi(
    /** The described path */
    val path: String,
    /** List of methods to describe **/
    val methods: Array<HttpMethod> = [GET],
    /** Schema version **/
    val versions: Array<String> = ["default"],
    /** Ignore the endpoint in the open api documentation */
    val ignore: Boolean = false,
    /** An optional, string summary, intended to apply to all operations in this path. **/
    val summary: String = NULL_STRING,
    /** An optional, string description, intended to apply to all operations in this path. **/
    val description: String = NULL_STRING,
    /**
     * Unique string used to identify the operation.
     * The id MUST be unique among all operations described in the API.
     * The operationId value is case-sensitive.
     *
     * You can also use [OpenApiOperation.AUTO_GENERATE]
     * if you want to generate the operationId automatically using the method name.
     **/
    val operationId: String = NULL_STRING,
    /** Declares this operation to be deprecated. Consumers SHOULD refrain from usage of the declared operation. **/
    val deprecated: Boolean = false,
    /**
     *  A list of tags for API documentation control.
     *  Tags can be used for logical grouping of operations by resources or any other qualifier.
     **/
    val tags: Array<String> = [],
    /** Describes applicable cookies */
    val cookies: Array<OpenApiParam> = [],
    /** Describes applicable headers */
    val headers: Array<OpenApiParam> = [],
    /** Describes applicable path parameters */
    val pathParams: Array<OpenApiParam> = [],
    /** Describes applicable query parameters */
    val queryParams: Array<OpenApiParam> = [],
    /** Describes applicable form parameters */
    val formParams: Array<OpenApiParam> = [],
    /**
     * The request body applicable for this operation.
     * The requestBody is only supported in HTTP methods where the HTTP 1.1 specification RFC7231 has explicitly defined semantics for request bodies.
     * In other cases where the HTTP spec is vague, requestBody SHALL be ignored by consumers.
     */
    val requestBody: OpenApiRequestBody = OpenApiRequestBody([]),
    /** Describes applicable callbacks */
    val callbacks: Array<OpenApiCallback> = [],
    // val composedRequestBody: OpenApiComposedRequestBody = OpenApiComposedRequestBody([]), ?
    /** The list of possible responses as they are returned from executing this operation. */
    val responses: Array<OpenApiResponse> = [],
    /** A declaration of which security mechanisms can be used for this operation. */
    val security: Array<OpenApiSecurity> = [],
)

fun OpenApi.getFormattedPath(): String =
    when {
        !path.startsWith("/") -> "/$path"
        else -> path
    }

/** Utility annotation to aggregate multiple [OpenApi] instances */
@Target(CLASS, FIELD, FUNCTION)
@Retention(RUNTIME)
annotation class OpenApis(
    val value: Array<OpenApi> = []
)

@Target()
@Retention(RUNTIME)
annotation class OpenApiResponse(
    val status: String,
    val content: Array<OpenApiContent> = [],
    val description: String = NULL_STRING
)

@Target()
@Retention(RUNTIME)
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
@Retention(RUNTIME)
annotation class OpenApiRequestBody(
    val content: Array<OpenApiContent>,
    val required: Boolean = false,
    val description: String = NULL_STRING
)

@Target()
@Retention(RUNTIME)
annotation class OpenApiCallback(
    val name: String,
    val url: String,
    val method: HttpMethod,
    val summary: String = NULL_STRING,
    val description: String = NULL_STRING,
    val requestBody: OpenApiRequestBody,
    val responses: Array<OpenApiResponse>
)

@Target()
@Retention(RUNTIME)
annotation class OpenApiContent(
    val from: KClass<*> = NULL_CLASS::class,
    val mimeType: String = ContentType.AUTODETECT,
    val type: String = NULL_STRING,
    val format: String = NULL_STRING,
    val properties: Array<OpenApiContentProperty> = []
)

@Target()
@Retention(RUNTIME)
annotation class OpenApiContentProperty(
    val name: String,
    val isArray: Boolean = false,
    val type: String,
    val format: String = NULL_STRING
)

@Target()
@Retention(RUNTIME)
annotation class OpenApiSecurity(
    val name: String,
    val scopes: Array<String> = []
)

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
@Retention(RUNTIME)
annotation class OpenApiIgnore

@Target(CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
@Retention(RUNTIME)
annotation class OpenApiName(
    val value: String
)

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
@Retention(RUNTIME)
annotation class OpenApiExample(
    val value: String
)

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
@Retention(RUNTIME)
@CustomAnnotation
annotation class OpenApiNullable(
    val nullable: Boolean = true
)

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, CLASS)
@Retention(RUNTIME)
annotation class OpenApiDescription(
    val value: String
)

enum class Nullability {
    NULLABLE,
    NOT_NULL,
    AUTO
}

@Target(CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
@Retention(RUNTIME)
annotation class OpenApiPropertyType(
    val definedBy: KClass<*>,
    val nullability: Nullability = Nullability.AUTO
)

enum class Visibility(val priority: Int) {
    PUBLIC(4),
    DEFAULT(3),
    PROTECTED(2),
    PRIVATE(1)
}

@Target(CLASS)
@Retention(RUNTIME)
annotation class OpenApiByFields(
    val value: Visibility = PUBLIC
)

/** Null class because annotations do not support null values */
@Suppress("ClassName")
class NULL_CLASS

/** Null string because annotations do not support null values */
const val NULL_STRING = "-- This string represents a null value and shouldn't be used --"

object OpenApiOperation {
    /** Value to use for auto-generate operationId */
    const val AUTO_GENERATE = "-- Auto-generate operationId on the fly. If you see this message you are either inspecting via debugger or something went wrong --"
}

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

class OpenApiLoader {

    fun loadOpenApiSchemes(): Map<String, String> =
        loadVersions().associateWith { loadVersion(it) ?: "{}" }

    fun loadVersions(): Set<String> =
        OpenApiLoader::class.java.getResourceAsStream("/openapi-plugin/.index")
            ?.readAllBytes()
            ?.decodeToString()
            ?.split("\n")
            ?.asSequence()
            ?.map { it.trim() }
            ?.map { it.removePrefix("openapi-") }
            ?.map { it.removeSuffix(".json") }
            ?.toSet()
            ?: emptySet()

    fun loadVersion(version: String): String? =
        OpenApiLoader::class.java.getResource("/openapi-plugin/openapi-$version.json")?.readText()

}
