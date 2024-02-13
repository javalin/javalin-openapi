package io.javalin.openapi

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.function.Consumer

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
    open var `in`: String = "header",
    open var name: String = "X-API-Key"
) : SecurityScheme {
    override val type: String = "apiKey"
}

class CookieAuth @JvmOverloads constructor(
    override var name: String,
    override var `in`: String = "cookie"
) : ApiKeyAuth()

class OpenID (val openIdConnectUrl: String) : SecurityScheme {
    override val type: String = "openIdConnect"
}

class OAuth2 @JvmOverloads constructor(
    var description: String,
    val flows: MutableMap<String, OAuth2Flow<*>> = mutableMapOf(),
) : SecurityScheme {
    override val type: String = "oauth2"

    fun withFlow(flow: OAuth2Flow<*>): OAuth2 = also {
        flows[flow.flowType] = flow
    }

    @JvmOverloads
    fun withAuthorizationCodeFlow(authorizationUrl: String, tokenUrl: String, flow: Consumer<AuthorizationCodeFlow> = Consumer {}): OAuth2 =
        withFlow(AuthorizationCodeFlow(authorizationUrl, tokenUrl).also { flow.accept(it) })

    @JvmOverloads
    fun withImplicitFlow(authorizationUrl: String, flow: Consumer<ImplicitFlow> = Consumer {}): OAuth2 =
        withFlow(ImplicitFlow(authorizationUrl).also { flow.accept(it) })

    @JvmOverloads
    fun withPasswordFlow(tokenUrl: String, flow: Consumer<PasswordFlow> = Consumer {}): OAuth2 =
        withFlow(PasswordFlow(tokenUrl).also { flow.accept(it) })

    @JvmOverloads
    fun withClientCredentials(tokenUrl: String, flow: Consumer<ClientCredentials> = Consumer {}): OAuth2 =
        withFlow(ClientCredentials(tokenUrl).also { flow.accept(it) })

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
    var authorizationUrl: String,
    var tokenUrl: String,
    override var scopes: MutableMap<String, String> = mutableMapOf()
) : OAuth2Flow<AuthorizationCodeFlow> {
    override val flowType: String = "authorizationCode"
}

class ImplicitFlow @JvmOverloads constructor(
    var authorizationUrl: String,
    override val scopes: MutableMap<String, String> = mutableMapOf()
) : OAuth2Flow<ImplicitFlow> {
    override val flowType: String = "implicit"
}

class PasswordFlow @JvmOverloads constructor(
    var tokenUrl: String,
    override val scopes: MutableMap<String, String> = mutableMapOf()
) : OAuth2Flow<PasswordFlow> {
    override val flowType: String = "password"
}

class ClientCredentials @JvmOverloads constructor(
    var tokenUrl: String,
    override val scopes: MutableMap<String, String> = mutableMapOf()
) : OAuth2Flow<ClientCredentials> {
    override val flowType: String = "clientCredentials"
}