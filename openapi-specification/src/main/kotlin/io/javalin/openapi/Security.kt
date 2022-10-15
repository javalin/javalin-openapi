package io.javalin.openapi

import com.fasterxml.jackson.annotation.JsonIgnore

data class Security @JvmOverloads constructor(
    val name: String,
    val scopes: MutableList<String> = mutableListOf()
) {

    fun withScope(scope: String): Security = also {
        scopes.add(scope)
    }

}

interface SecurityScheme {
    val type: String
}

abstract class HttpAuth(val scheme: String) : SecurityScheme {
    override val type: String = "http"
}

class BasicAuth : HttpAuth(scheme = "basic")

class BearerAuth : HttpAuth(scheme = "bearer")

open class ApiKeyAuth(
    open val `in`: String = "header",
    open val name: String = "X-API-Key"
) : SecurityScheme {
    override val type: String = "apiKey"
}

class CookieAuth @JvmOverloads constructor(
    override val name: String,
    override val `in`: String = "cookie"
) : ApiKeyAuth()

class OpenID (val openIdConnectUrl: String) : SecurityScheme {
    override val type: String = "openIdConnect"
}

class OAuth2 @JvmOverloads constructor(
    val description: String,
    val flows: MutableMap<String, OAuth2Flow<*>> = mutableMapOf(),
) : SecurityScheme {
    override val type: String = "oauth2"

    fun withFlow(flow: OAuth2Flow<*>): OAuth2 = also {
        flows[flow.flowType] = flow
    }
}

interface OAuth2Flow<I : OAuth2Flow<I>> {
    @get:JsonIgnore
    val flowType: String
    val scopes: MutableMap<String, String>

    @Suppress("UNCHECKED_CAST")
    fun withScope(scope: String, description: String): I = also {
        scopes[scope] = description
    } as I
}

class AuthorizationCodeFlow @JvmOverloads constructor(
    val authorizationUrl: String,
    val tokenUrl: String,
    override val scopes: MutableMap<String, String> = mutableMapOf()
) : OAuth2Flow<AuthorizationCodeFlow> {
    override val flowType: String = "authorizationCode"
}

class ImplicitFlow @JvmOverloads constructor(
    val authorizationUrl: String,
    override val scopes: MutableMap<String, String> = mutableMapOf()
) : OAuth2Flow<ImplicitFlow> {
    override val flowType: String = "implicit"
}

class PasswordFlow @JvmOverloads constructor(
    val tokenUrl: String,
    override val scopes: MutableMap<String, String> = mutableMapOf()
) : OAuth2Flow<PasswordFlow> {
    override val flowType: String = "password"
}

class ClientCredentials @JvmOverloads constructor(
    val tokenUrl: String,
    override val scopes: MutableMap<String, String> = mutableMapOf()
) : OAuth2Flow<ClientCredentials> {
    override val flowType: String = "clientCredentials "
}