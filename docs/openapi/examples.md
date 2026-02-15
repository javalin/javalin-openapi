# Examples

Provide example values in your OpenAPI schema using `@OpenApiExample` on properties or `@OpenApiExampleProperty` on content.

## Property Examples

Use `@OpenApiExample` on getters or fields:

```kotlin
class User {
    @OpenApiExample("John Doe")
    val name: String = ""

    @OpenApiExample("john@example.com")
    val email: String = ""
}
```

Generates `"example": "John Doe"` on each property in the schema.

## Raw Examples

For non-string values (numbers, booleans, arrays, objects), use `raw`. The value is parsed as JSON:

```kotlin
class Config {
    @OpenApiExample(raw = "8080")
    val port: Int = 0

    @OpenApiExample(raw = "true")
    val enabled: Boolean = false

    @OpenApiExample(raw = "[1, 2, 3]")
    val ids: List<Int> = emptyList()
}
```

Generates typed examples: `"example": 8080`, `"example": true`, `"example": [1, 2, 3]`.

## Content-level Examples

Use `exampleObjects` on `@OpenApiContent` for structured examples:

```kotlin
@OpenApiContent(
    from = String::class,
    exampleObjects = [
        OpenApiExampleProperty(
            name = "username",
            value = "johndoe"
        ),
        OpenApiExampleProperty(
            name = "role",
            value = "admin"
        )
    ]
)
```

## Nested Examples

`@OpenApiExampleProperty` supports nesting via `objects`. Entries without a `name` are treated as array elements:

```kotlin
@OpenApiContent(
    from = String::class,
    exampleObjects = [
        OpenApiExampleProperty(
            name = "name",
            value = "document"
        ),
        OpenApiExampleProperty(
            name = "metadata",
            objects = [
                OpenApiExampleProperty(
                    name = "author",
                    value = "John Doe"
                ),
                OpenApiExampleProperty(
                    name = "tags",
                    objects = [
                        OpenApiExampleProperty(
                            value = "important"
                        ),
                        OpenApiExampleProperty(
                            value = "research"
                        )
                    ]
                )
            ]
        )
    ]
)
```

Generates:

```json
{
  "example": {
    "name": "document",
    "metadata": {
      "author": "John Doe",
      "tags": ["important", "research"]
    }
  }
}
```

## Dictionary Examples

Combine `additionalProperties` with `exampleObjects` for map structures:

```kotlin
@OpenApiContent(
    mimeType = "application/json",
    additionalProperties = OpenApiAdditionalContent(
        from = String::class,
        exampleObjects = [
            OpenApiExampleProperty(
                name = "key1",
                value = "value1"
            ),
            OpenApiExampleProperty(
                name = "key2",
                value = "value2"
            )
        ]
    )
)
```
