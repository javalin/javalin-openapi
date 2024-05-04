package io.javalin.openapi.data

import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApiCallback
import io.javalin.openapi.OpenApiOperation
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiRequestBody
import io.javalin.openapi.OpenApiResponse
import io.javalin.openapi.OpenApiSecurity

class OpenApiDocumentation {

    internal class DocumentationState {
        var path: String? = null
        var methods: List<HttpMethod>? = null
        var versions: List<String>? = listOf("default")
        var ignore: Boolean? = false
        var summary: String? = null
        var description: String? = null
        var operationId: String? = null
        var deprecated: Boolean? = null
        var tags: List<String>? = null
        var cookies: List<OpenApiParam>? = null
        var headers: List<OpenApiParam>? = null
        var pathParams: List<OpenApiParam>? = null
        var queryParams: List<OpenApiParam>? = null
        var formParams: List<OpenApiParam>? = null
        var requestBody: OpenApiRequestBody? = null
        var callbacks: List<OpenApiCallback>? = null
        var responses: List<OpenApiResponse>? = null
        var security: List<OpenApiSecurity>? = null
    }

    internal val state = DocumentationState()

    /** The described path */
    fun path(path: String) = apply { this.state.path = path }

    /** List of methods to describe **/
    fun methods(vararg methods: HttpMethod) = apply { this.state.methods = methods.toList() }

    /** Schema version **/
    fun versions(vararg versions: String) = apply { this.state.versions = versions.toList() }

    /** Ignore the endpoint in the open api documentation */
    fun ignore(ignore: Boolean) = apply { this.state.ignore = ignore }

    /** An optional, string summary, intended to apply to all operations in this path. **/
    fun summary(summary: String) = apply { this.state.summary = summary }

    /** An optional, string description, intended to apply to all operations in this path. **/
    fun description(description: String) = apply { this.state.description = description }

    /**
     * Unique string used to identify the operation.
     * The id MUST be unique among all operations described in the API.
     * The operationId value is case-sensitive.
     *
     * You can also use [OpenApiOperation.AUTO_GENERATE]
     * if you want to generate the operationId automatically using the method name.
     **/
    fun operationId(operationId: String) = apply { this.state.operationId = operationId }

    /** Declares this operation to be deprecated. Consumers SHOULD refrain from usage of the declared operation. **/
    fun deprecated(deprecated: Boolean) = apply { this.state.deprecated = deprecated }

    /**
     *  A list of tags for API documentation control.
     *  Tags can be used for logical grouping of operations by resources or any other qualifier.
     **/
    fun tags(vararg tags: String) = apply { this.state.tags = tags.toList() }

    /** Describes applicable cookies */
    fun cookies(vararg cookies: OpenApiParam) = apply { this.state.cookies = cookies.toList() }

    /** Describes applicable headers */
    fun headers(vararg headers: OpenApiParam) = apply { this.state.headers = headers.toList() }

    /** Describes applicable path parameters */
    fun pathParams(vararg pathParams: OpenApiParam) = apply { this.state.pathParams = pathParams.toList() }

    /** Describes applicable query parameters */
    fun queryParams(vararg queryParams: OpenApiParam) = apply { this.state.queryParams = queryParams.toList() }

    /** Describes applicable form parameters */
    fun formParams(vararg formParams: OpenApiParam) = apply { this.state.formParams = formParams.toList() }

    /**
     * The request body applicable for this operation.
     * The requestBody is only supported in HTTP methods where the HTTP 1.1 specification RFC7231 has explicitly defined semantics for request bodies.
     * In other cases where the HTTP spec is vague, requestBody SHALL be ignored by consumers.
     */
    fun requestBody(requestBody: OpenApiRequestBody) = apply { this.state.requestBody = requestBody }

    /** Describes applicable callbacks */
    fun callbacks(vararg callbacks: OpenApiCallback) = apply { this.state.callbacks = callbacks.toList() }

    /** The list of possible responses as they are returned from executing this operation. */
    fun responses(vararg responses: OpenApiResponse) = apply { this.state.responses = responses.toList() }

    /** A declaration of which security mechanisms can be used for this operation. */
    fun security(vararg security: OpenApiSecurity) = apply { this.state.security = security.toList() }
}