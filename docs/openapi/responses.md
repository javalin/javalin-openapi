# Responses

Use `@OpenApiResponse` to describe the possible responses from an endpoint.

## Basic Response

```kotlin
@OpenApi(
    path = "/users/{id}",
    responses = [
        OpenApiResponse(
            status = "200",
            content = [OpenApiContent(from = User::class)]
        ),
        OpenApiResponse(
            status = "404",
            description = "User not found"
        )
    ]
)
```

## Multiple Content Types

```kotlin
OpenApiResponse(
    status = "200",
    content = [
        OpenApiContent(
            from = User::class,
            mimeType = "application/json"
        ),
        OpenApiContent(
            from = User::class,
            mimeType = "application/xml"
        )
    ]
)
```

## Response Headers

```kotlin
OpenApiResponse(
    status = "200",
    content = [OpenApiContent(from = User::class)],
    headers = [
        OpenApiParam(
            name = "X-Total-Count",
            type = Int::class,
            description = "Total results"
        ),
        OpenApiParam(
            name = "X-Request-ID",
            description = "Request tracking ID"
        )
    ]
)
```

## Array Responses

```kotlin
OpenApiResponse(
    status = "200",
    content = [
        OpenApiContent(from = Array<User>::class)
    ],
    description = "List of users"
)
```

## @OpenApiResponse Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `status` | `String` | — | HTTP status code (required) |
| `content` | `OpenApiContent[]` | `[]` | Response content |
| `description` | `String` | `""` | Response description |
| `headers` | `OpenApiParam[]` | `[]` | Response headers |
