# Schema Generation

The annotation processor automatically generates OpenAPI component schemas from your Java/Kotlin classes. This page covers type mappings and property-level annotations that control schema output.

## Type Mappings

### Primitive & Common Types

| Java Type | OpenAPI Type | Format |
|-----------|-------------|--------|
| `boolean` / `Boolean` | `boolean` | — |
| `byte` / `Byte` | `integer` | `int32` |
| `short` / `Short` | `integer` | `int32` |
| `int` / `Integer` | `integer` | `int32` |
| `long` / `Long` | `integer` | `int64` |
| `float` / `Float` | `number` | `float` |
| `double` / `Double` | `number` | `double` |
| `char` / `Character` | `string` | — |
| `String` | `string` | — |
| `BigDecimal` | `string` | — |
| `UUID` | `string` | — |

### Date & Time Types

| Java Type | OpenAPI Type | Format |
|-----------|-------------|--------|
| `java.util.Date` | `string` | `date` |
| `LocalDate` | `string` | `date` |
| `LocalDateTime` | `string` | `date-time` |
| `Instant` | `string` | `date-time` |

### Binary Types

| Java Type | OpenAPI Type | Format |
|-----------|-------------|--------|
| `byte[]` | `string` | `binary` |
| `InputStream` | `string` | `binary` |
| `File` | `string` | `binary` |

### Collections & Maps

| Java Type | OpenAPI Representation |
|-----------|----------------------|
| `List<T>` / `T[]` | `{ "type": "array", "items": { ... } }` |
| `Map<K, V>` | `{ "type": "object", "additionalProperties": { ... } }` |
| `Object` | `{ "type": "object" }` |

## Property Resolution

By default, properties are resolved from getter methods following JavaBean conventions. The `get` / `is` prefix is stripped:

- `getName()` → `name`
- `isActive()` → `active`

### Java Records

Record components are used directly as properties.

### Field-based Resolution

Use `@OpenApiByFields` to resolve properties from fields instead of getters:

```kotlin
@OpenApiByFields(value = Visibility.PUBLIC)
class Config {
    val host: String = ""
    val port: Int = 0
}
```

The `value` parameter controls the minimum field visibility. Use `only = true` to ignore methods entirely.

## Property Annotations

### @OpenApiIgnore

Exclude a property from the schema:

```kotlin
@OpenApiIgnore
fun getInternalId(): String = ...
```

### @OpenApiRequired

Force a property to appear in the `required` array:

```kotlin
@OpenApiRequired
val name: String = ""
```

### @OpenApiName

Override the property name in the schema:

```kotlin
@OpenApiName("user_name")
val name: String = ""
```

### @OpenApiDescription

Add a description to a property or class:

```kotlin
@OpenApiDescription("The user's display name")
val name: String = ""
```

### @OpenApiPropertyType

Override the schema type for a property:

```kotlin
@OpenApiPropertyType(definedBy = String::class)
val id: ObjectId = ObjectId()
```

### @OpenApiNullable

Mark a property as nullable:

```kotlin
@OpenApiNullable
val middleName: String? = null
```

## Recursive Types

Self-referencing types are handled via `$ref`:

```kotlin
class TreeNode {
    val value: String = ""
    val left: TreeNode? = null
    val right: TreeNode? = null
}
```

The `left` and `right` properties reference `#/components/schemas/TreeNode`.

## Generic Types

Generic type parameters are resolved when used in concrete contexts. `Page<User>` resolves `items` as an array of `User`.

## Custom Type Mappings

Register custom type mappings in the [compile-time configuration](../advanced/configuration):

```groovy
configuration.simpleTypeMappings['org.bson.types.ObjectId'] = new SimpleType("string")
```
