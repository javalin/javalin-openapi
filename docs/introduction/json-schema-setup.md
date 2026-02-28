# Setup without Javalin

The annotation processor works without Javalin or any web framework:

- Generates **OpenAPI 3.1.0** specifications via `@OpenApi`
- Generates standalone **[JSON Schema 2020-12](https://json-schema.org/draft/2020-12)** documents via `@JsonSchema`
- All generated files are bundled as **classpath resources** in your JAR

You can serve them with any HTTP server, use them for validation, feed them to code generators, or process them however you like.

## Installation

You only need the annotation processor and the specification module — no Javalin plugins required:

::: code-group

```kotlin [Gradle (Kotlin)]
dependencies {
    val openapi = "7.0.1"

    annotationProcessor(
        "io.javalin.community.openapi:openapi-annotation-processor:$openapi"
    )
    implementation(
        "io.javalin.community.openapi:openapi-specification:$openapi"
    )
}
```

```kotlin [Gradle (Kotlin) with Kapt]
dependencies {
    val openapi = "7.0.1"

    kapt(
        "io.javalin.community.openapi:openapi-annotation-processor:$openapi"
    )
    implementation(
        "io.javalin.community.openapi:openapi-specification:$openapi"
    )
}
```

```xml [Maven]
<dependencies>
    <dependency>
        <groupId>io.javalin.community.openapi</groupId>
        <artifactId>openapi-specification</artifactId>
        <version>7.0.1</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>io.javalin.community.openapi</groupId>
                        <artifactId>openapi-annotation-processor</artifactId>
                        <version>7.0.1</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

:::

## Annotate Your Types

Use `@JsonSchema` on any class to generate a standalone schema:

```kotlin
@JsonSchema
class UserConfig {
    val name: String = ""
    val port: Int = 0
    val hosts: List<String> = emptyList()
}
```

At compile time, this generates a `/json-schemes/com.example.UserConfig` resource file:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "name": { "type": "string" },
    "port": {
      "type": "integer",
      "format": "int32"
    },
    "hosts": {
      "type": "array",
      "items": { "type": "string" }
    }
  },
  "required": ["name", "port", "hosts"]
}
```

### Differences between the Two Modes

| Aspect | OpenAPI | JSON Schema |
|--------|---------|-------------|
| Output format | OpenAPI 3.1.0 | JSON Schema 2020-12 |
| Scope | Endpoint docs | Standalone type schemas |
| References | `$ref` | All types inlined |
| Trigger | `@OpenApi` | `@JsonSchema` |

Both modes are framework-agnostic at the annotation processing level — Javalin is only needed if you want to serve the specs via the Javalin plugins.

## Loading OpenAPI Specs at Runtime

Use `OpenApiLoader` to load the generated OpenAPI specifications from your classpath:

```kotlin
val loader = OpenApiLoader()

// version → JSON
val schemes = loader.loadOpenApiSchemes()

for ((version, json) in schemes) {
    println(version) // "default"
    println(json)    // OpenAPI JSON
}
```

## Loading JSON Schemas at Runtime

Use `JsonSchemaLoader` to load the generated JSON schemas from your classpath:

```kotlin
val loader = JsonSchemaLoader()
val schemas = loader.loadGeneratedSchemes()

for (schema in schemas) {
    println(schema.name)
    println(schema.contentAsString)
}
```

## Configuration

### Required Properties

By default, all non-null fields are marked as `required`. Disable this per type:

```kotlin
@JsonSchema(requireNonNulls = false)
class OptionalConfig {
    val name: String = ""  // not required anymore
}
```

### Resource Generation

If you only need `@JsonSchema` as a marker for OpenAPI composition but don't want standalone resource files, disable resource generation:

```kotlin
@JsonSchema(generateResource = false)
class InternalEntity
```

## Shared Annotations

Most property-level annotations work in both OpenAPI and JSON Schema modes:

- `@OpenApiName` — override property names
- `@OpenApiDescription` — add descriptions
- `@OpenApiIgnore` — exclude properties
- `@OpenApiRequired` — force required
- `@OpenApiPropertyType` — override types
- `@OpenApiNullable` — mark nullable
- `@OpenApiNaming` — apply naming strategies
- `@OpenApiByFields` — resolve from fields instead of getters
- Validation annotations — `@OpenApiNumberValidation`, `@OpenApiStringValidation`, `@OpenApiArrayValidation`, `@OpenApiObjectValidation`

## Next Steps

- [Type Composition](../json-schema/getting-started) — `@OneOf`, `@AnyOf`, `@AllOf` with discriminators
- [Custom Properties](../json-schema/custom-properties) — add custom schema properties with `@Custom`
