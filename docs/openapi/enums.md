# Enums

Enums are mapped to `string` schemas with an `enum` array listing all values.

## Basic Enum

```kotlin
enum class Status {
    ACTIVE,
    INACTIVE,
    PENDING
}
```

Generates:

```json
{
  "Status": {
    "type": "string",
    "enum": ["ACTIVE", "INACTIVE", "PENDING"]
  }
}
```

## Custom Value Names

Use `@OpenApiName` on individual enum values:

```kotlin
enum class Priority {
    @OpenApiName("low") LOW,
    @OpenApiName("medium") MEDIUM,
    @OpenApiName("high") HIGH
}
```

Generates `"enum": ["low", "medium", "high"]`.

## Naming Strategies

Apply `@OpenApiNaming` to transform all value names automatically:

```kotlin
@OpenApiNaming(OpenApiNamingStrategy.KEBAB_CASE)
enum class ErrorCode {
    NOT_FOUND,     // → "not-found"
    BAD_REQUEST,   // → "bad-request"
    SERVER_ERROR   // → "server-error"
}
```

See [Naming Strategies](./naming) for details.

## Integer Enums

Combine `@OpenApiPropertyType` with `@OpenApiName` to create integer enums. The `@OpenApiName` values are parsed as the target type:

```kotlin
@OpenApiPropertyType(definedBy = Int::class)
@OpenApiDescription(
    "Sort order:\n * 1 - Ascending\n * 2 - Descending"
)
enum class SortOrder {
    @OpenApiName("1") ASCENDING,
    @OpenApiName("2") DESCENDING
}
```

Generates:

```json
{
  "SortOrder": {
    "type": "integer",
    "format": "int32",
    "enum": [1, 2],
    "description": "Sort order:\n * 1 - Ascending\n * 2 - Descending"
  }
}
```

## Enum Descriptions

Add a description with `@OpenApiDescription`:

```kotlin
@OpenApiDescription("The current status of the resource")
enum class Status {
    ACTIVE,
    INACTIVE
}
```

Generates:

```json
{
  "Status": {
    "type": "string",
    "enum": ["ACTIVE", "INACTIVE"],
    "description": "The current status of the resource"
  }
}
```
