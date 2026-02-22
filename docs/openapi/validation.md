# Validation

Add OpenAPI schema validation constraints to properties using validation annotations. These annotations work in both OpenAPI and JSON Schema modes.

## Number Validation

```kotlin
@OpenApiNumberValidation(
    minimum = "0",
    maximum = "10000",
    exclusiveMaximum = "10000",
    multipleOf = "0.01"
)
val price: Double = 0.0
```

In OAS 3.1.0, `exclusiveMinimum` and `exclusiveMaximum` are standalone numeric values (not booleans). Use `minimum` for inclusive bounds and `exclusiveMinimum`/`exclusiveMaximum` when the bound itself should be excluded.

| Property | Description |
|----------|-------------|
| `minimum` | Minimum value (inclusive) |
| `maximum` | Maximum value (inclusive) |
| `exclusiveMinimum` | Exclusive minimum value (OAS 3.1.0 numeric) |
| `exclusiveMaximum` | Exclusive maximum value (OAS 3.1.0 numeric) |
| `multipleOf` | Must be a multiple of this |

## String Validation

```kotlin
@OpenApiStringValidation(
    minLength = "1",
    maxLength = "100",
    pattern = "^[a-zA-Z0-9_]+$"
)
val username: String = ""

@OpenApiStringValidation(format = "email")
val email: String = ""
```

| Property | Description |
|----------|-------------|
| `minLength` | Minimum string length |
| `maxLength` | Maximum string length |
| `pattern` | Regex pattern |
| `format` | String format (`email`, `uri`, ...) |

## Array Validation

```kotlin
@OpenApiArrayValidation(
    minItems = "1",
    maxItems = "50",
    uniqueItems = true
)
val tags: List<String> = emptyList()
```

| Property | Description |
|----------|-------------|
| `minItems` | Minimum number of items |
| `maxItems` | Maximum number of items |
| `uniqueItems` | All items must be unique |

## Object Validation

```kotlin
@OpenApiObjectValidation(
    minProperties = "1",
    maxProperties = "20"
)
val settings: Map<String, String> = emptyMap()
```

| Property | Description |
|----------|-------------|
| `minProperties` | Min properties |
| `maxProperties` | Max properties |

## Combining Annotations

Validation annotations combine with other property annotations:

```kotlin
@OpenApiStringValidation(
    minLength = "1",
    maxLength = "200"
)
@OpenApiRequired
@OpenApiDescription("Search query string")
val query: String = ""

@OpenApiNumberValidation(
    minimum = "1",
    maximum = "100"
)
@OpenApiExample(raw = "20")
val pageSize: Int = 20
```
