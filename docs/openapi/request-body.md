# Request Body

Use `@OpenApiRequestBody` and `@OpenApiContent` to describe request bodies.

## Basic Request Body

```kotlin
@OpenApi(
    path = "/users",
    methods = [HttpMethod.POST],
    requestBody = OpenApiRequestBody(
        content = [
            OpenApiContent(from = CreateUserRequest::class)
        ],
        required = true,
        description = "User data to create"
    )
)
```

## Multiple Content Types

```kotlin
@OpenApiRequestBody(
    content = [
        OpenApiContent(
            from = UserData::class,
            mimeType = "application/json"
        ),
        OpenApiContent(
            from = UserData::class,
            mimeType = "application/xml"
        )
    ]
)
```

## File Upload

```kotlin
@OpenApiRequestBody(
    content = [
        OpenApiContent(
            from = InputStream::class,
            mimeType = "application/octet-stream"
        )
    ]
)
```

## Content Type Auto-detection

When `mimeType` is not specified, the content type is auto-detected from the `from` type:

- Object types → `application/json`
- `String` → `text/plain`
- `ByteArray`, `InputStream`, `File` → `application/octet-stream`

## Inline Properties

Define schema properties directly without creating a separate class:

```kotlin
@OpenApiContent(
    properties = [
        OpenApiContentProperty(
            name = "username",
            type = "string"
        ),
        OpenApiContentProperty(
            name = "age",
            type = "integer",
            format = "int32"
        )
    ]
)
```

## Dictionary / Map Content

Describe map-like structures with `additionalProperties`:

```kotlin
@OpenApiContent(
    mimeType = "application/json",
    additionalProperties = OpenApiAdditionalContent(
        from = String::class,
        exampleObjects = [
            OpenApiExampleProperty(
                name = "key1",
                value = "value1"
            )
        ]
    )
)
```

Generates:

```json
{
  "type": "object",
  "additionalProperties": { "type": "string" },
  "example": { "key1": "value1" }
}
```

## @OpenApiContent Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `from` | `KClass<*>` | — | Schema type |
| `mimeType` | `String` | Auto-detect | Content type |
| `type` | `String` | — | Override type |
| `format` | `String` | — | Override format |
| `properties` | `OpenApiContentProperty[]` | `[]` | Inline properties |
| `additionalProperties` | `OpenApiAdditionalContent` | — | Map value type |
| `example` | `String` | — | Example value |
| `exampleObjects` | `OpenApiExampleProperty[]` | `[]` | Structured example |
