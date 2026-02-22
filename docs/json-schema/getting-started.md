# Getting Started with JSON Schema

The `@JsonSchema` annotation generates standalone [JSON Schema 2020-12](https://json-schema.org/draft/2020-12) documents for individual types. Unlike OpenAPI schemas which use `$ref` references, JSON Schema output is fully self-contained with all nested types inlined.

## Annotate a Type

```kotlin
@JsonSchema
class ServerConfig {
    val host: String = ""
    val port: Int = 0
    val ssl: Boolean = false
    val allowedOrigins: List<String> = emptyList()
}
```

## Generated Output

At compile time, this produces a `/json-schemes/com.example.ServerConfig` resource file:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "host": { "type": "string" },
    "port": { "type": "integer", "format": "int32" },
    "ssl": { "type": "boolean" },
    "allowedOrigins": {
      "type": "array",
      "items": { "type": "string" }
    }
  },
  "required": ["host", "port", "ssl", "allowedOrigins"]
}
```

All nested types are inlined directly — there are no `$ref` references.

## Loading at Runtime

```kotlin
val loader = JsonSchemaLoader()
val schemas = loader.loadGeneratedSchemes()

for (schema in schemas) {
    val name = schema.name                 // "com.example.ServerConfig"
    val json = schema.contentAsString      // the JSON schema
}
```

## Options

### Required Properties

By default, all non-null fields are marked as `required`. Disable per type:

```kotlin
@JsonSchema(requireNonNulls = false)
class OptionalConfig {
    val name: String = ""  // not in "required" array
}
```

### Disable Resource Generation

Use `@JsonSchema` as a marker without generating resource files:

```kotlin
@JsonSchema(generateResource = false)
class InternalEntity
```

## Property Annotations

All property-level `@OpenApi*` annotations also apply to JSON Schema output:

- `@OpenApiName` — override property names
- `@OpenApiDescription` — add descriptions
- `@OpenApiIgnore` — exclude properties
- `@OpenApiRequired` — force required
- `@OpenApiPropertyType` — override types
- `@OpenApiNullable` — mark nullable
- `@OpenApiNaming` — naming strategies (snake_case, kebab-case)
- `@OpenApiByFields` — resolve from fields instead of getters
- `@OpenApiNumberValidation`, `@OpenApiStringValidation`, `@OpenApiArrayValidation`, `@OpenApiObjectValidation` — validation constraints
