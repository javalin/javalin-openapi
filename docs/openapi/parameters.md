# Parameters

Use `@OpenApiParam` to describe path, query, header, cookie, and form parameters.

## Path Parameters

```kotlin
@OpenApi(
    path = "/users/{userId}/posts/{postId}",
    pathParams = [
        OpenApiParam(
            name = "userId",
            type = Long::class,
            required = true,
            description = "The user ID"
        ),
        OpenApiParam(
            name = "postId",
            type = Long::class,
            required = true
        )
    ]
)
```

## Query Parameters

```kotlin
@OpenApi(
    path = "/users",
    queryParams = [
        OpenApiParam(
            name = "page",
            type = Int::class,
            description = "Page number"
        ),
        OpenApiParam(
            name = "size",
            type = Int::class,
            description = "Page size"
        ),
        OpenApiParam(
            name = "search",
            description = "Search query"
        )
    ]
)
```

## Header Parameters

```kotlin
@OpenApi(
    path = "/users",
    headers = [
        OpenApiParam(
            name = "X-Request-ID",
            required = true
        ),
        OpenApiParam(
            name = "X-Custom-Header",
            deprecated = true
        )
    ]
)
```

## Cookie Parameters

```kotlin
@OpenApi(
    path = "/dashboard",
    cookies = [
        OpenApiParam(
            name = "session_id",
            required = true
        )
    ]
)
```

## Form Parameters

```kotlin
@OpenApi(
    path = "/login",
    methods = [HttpMethod.POST],
    formParams = [
        OpenApiParam(
            name = "username",
            required = true
        ),
        OpenApiParam(
            name = "password",
            required = true
        )
    ]
)
```

## @OpenApiParam Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `name` | `String` | — | Parameter name (required) |
| `type` | `KClass<*>` | `String::class` | Parameter type |
| `description` | `String` | — | Description |
| `deprecated` | `Boolean` | `false` | Mark as deprecated |
| `required` | `Boolean` | `false` | Mark as required |
| `allowEmptyValue` | `Boolean` | `false` | Allow empty values |
| `example` | `String` | `""` | Example value |
