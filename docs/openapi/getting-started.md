# Getting Started with OpenAPI

Use the `@OpenApi` annotation on handler methods to describe your API endpoints. The annotation processor generates the OpenAPI specification at compile time.

## First Endpoint

```kotlin
@OpenApi(
    path = "/users/{userId}",
    methods = [HttpMethod.GET],
    summary = "Get user by ID",
    tags = ["Users"],
    pathParams = [
        OpenApiParam(
            name = "userId",
            type = Long::class,
            required = true
        )
    ],
    responses = [
        OpenApiResponse(
            status = "200",
            content = [OpenApiContent(from = User::class)],
            description = "The user"
        ),
        OpenApiResponse(
            status = "404",
            description = "User not found"
        )
    ]
)
fun getUser(ctx: Context) {
    // handler logic
}
```

Build your project and visit `/openapi` to see the generated specification, or `/swagger` / `/redoc` if you registered those plugins.

## @OpenApi Properties

| Property | Type | Description |
|----------|------|-------------|
| `path` | `String` | Endpoint path (required) |
| `methods` | `HttpMethod[]` | HTTP methods (default: `GET`) |
| `versions` | `String[]` | Schema versions |
| `ignore` | `Boolean` | Exclude from docs |
| `summary` | `String` | Short summary |
| `description` | `String` | Detailed description |
| `operationId` | `String` | Unique operation ID |
| `deprecated` | `Boolean` | Mark as deprecated |
| `tags` | `String[]` | Tags for grouping |
| `cookies` | `OpenApiParam[]` | Cookie parameters |
| `headers` | `OpenApiParam[]` | Header parameters |
| `pathParams` | `OpenApiParam[]` | Path parameters |
| `queryParams` | `OpenApiParam[]` | Query parameters |
| `requestBody` | `OpenApiRequestBody` | Request body |
| `responses` | `OpenApiResponse[]` | Response definitions |
| `security` | `OpenApiSecurity[]` | Security requirements |
| `callbacks` | `OpenApiCallback[]` | Callback definitions |

## Operation ID

Specify a custom operation ID or auto-generate one from the method name:

```kotlin
@OpenApi(
    path = "/users",
    operationId = "listUsers"
    // or: operationId = OpenApiOperation.AUTO_GENERATE
)
```

## Security

Reference security schemes defined in the [plugin configuration](../introduction/setup):

```kotlin
@OpenApi(
    path = "/users",
    security = [
        OpenApiSecurity(name = "BearerAuth")
    ]
)
```

## Versioning

`@OpenApi` is repeatable, so you can define multiple versions on the same handler:

```kotlin
@OpenApi(
    path = "/users",
    versions = ["v1"],
    responses = [
        OpenApiResponse(
            status = "200",
            content = [OpenApiContent(from = UserV1::class)]
        )
    ]
)
@OpenApi(
    path = "/users",
    versions = ["v2"],
    responses = [
        OpenApiResponse(
            status = "200",
            content = [OpenApiContent(from = UserV2::class)]
        )
    ]
)
fun getUsers(ctx: Context) { }
```

## Callbacks

Define webhook callbacks:

```kotlin
@OpenApi(
    path = "/subscribe",
    methods = [HttpMethod.POST],
    callbacks = [
        OpenApiCallback(
            name = "onEvent",
            url = "{${'$'}request.body#/callbackUrl}",
            method = HttpMethod.POST,
            summary = "Event notification",
            requestBody = OpenApiRequestBody(
                content = [OpenApiContent(from = Event::class)]
            ),
            responses = [
                OpenApiResponse(status = "200")
            ]
        )
    ]
)
```
