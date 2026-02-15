# Naming Strategies

Use `@OpenApiNaming` to automatically transform property and enum value names in the generated schema, similar to Jackson's `@JsonNaming`.

## Class-level Naming

Apply `@OpenApiNaming` on a class to transform all property names:

```kotlin
@OpenApiNaming(OpenApiNamingStrategy.SNAKE_CASE)
class UserProfile {
    val firstName: String = ""    // → "first_name"
    val lastName: String = ""     // → "last_name"
    val emailAddress: String = "" // → "email_address"
}
```

## Available Strategies

| Strategy | Input | Output |
|----------|-------|--------|
| `DEFAULT` | `firstName` | `firstName` (no change) |
| `SNAKE_CASE` | `firstName` | `first_name` |
| `KEBAB_CASE` | `firstName` | `first-name` |

## Per-property Override

`@OpenApiName` on a property always takes priority over the class-level strategy:

```kotlin
@OpenApiNaming(OpenApiNamingStrategy.SNAKE_CASE)
class UserProfile {
    val firstName: String = ""  // → "first_name" (from strategy)

    @OpenApiName("ID")
    val id: String = ""         // → "ID" (explicit override)
}
```

## Enum Naming

`@OpenApiNaming` also works on enum classes to transform value names:

```kotlin
@OpenApiNaming(OpenApiNamingStrategy.KEBAB_CASE)
enum class StatusCode {
    NOT_FOUND,       // → "not-found"
    BAD_REQUEST,     // → "bad-request"
    INTERNAL_ERROR   // → "internal-error"
}
```

Use `@OpenApiName` on individual values to override specific entries:

```kotlin
@OpenApiNaming(OpenApiNamingStrategy.KEBAB_CASE)
enum class StatusCode {
    NOT_FOUND,                       // → "not-found"

    @OpenApiName("custom-value")
    BAD_REQUEST,                     // → "custom-value"

    INTERNAL_ERROR                   // → "internal-error"
}
```

See [Enums](./enums) for more enum customization options.
