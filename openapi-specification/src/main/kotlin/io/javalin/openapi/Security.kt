package io.javalin.openapi

data class Security(
    val name: String,
    val scopes: List<String> = listOf()
)

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
    val flows: List<OAuth2Flow> = emptyList(),
) : SecurityScheme {
    override val type: String = "oauth2"
}

interface OAuth2Flow {
    val scopes: Map<String, String>
}

class AuthorizationCodeFlow @JvmOverloads constructor(
    val authorizationUrl: String,
    val tokenUrl: String,
    override val scopes: Map<String, String> = emptyMap()
) : OAuth2Flow

class ImplicitFlow @JvmOverloads constructor(
    val authorizationUrl: String,
    override val scopes: Map<String, String> = emptyMap()
) : OAuth2Flow

class PasswordFlow @JvmOverloads constructor(
    val tokenUrl: String,
    override val scopes: Map<String, String> = emptyMap()
) : OAuth2Flow

class ClientCredentials @JvmOverloads constructor(
    val tokenUrl: String,
    override val scopes: Map<String, String> = emptyMap()
) : OAuth2Flow