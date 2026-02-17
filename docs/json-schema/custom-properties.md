# Custom Properties

Add custom properties to your schemas using `@Custom` or by creating custom annotations with `@CustomAnnotation`.

## @Custom

Add individual custom properties to a class or field:

```kotlin
@Custom(name = "x-internal", value = "true")
@Custom(name = "x-category", value = "admin")
class InternalService {
    val name: String = ""
}
```

`@Custom` is repeatable — you can add multiple custom properties to the same element.

## @CustomAnnotation

Create reusable annotation types that group multiple custom properties together. Mark your annotation with `@CustomAnnotation`:

```kotlin
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FIELD,
    AnnotationTarget.CLASS
)
@CustomAnnotation
annotation class Description(
    val title: String,
    val description: String,
    val statusCode: Int
)
```

All properties of the custom annotation are added to the schema:

```kotlin
@Description(
    title = "User Entity",
    description = "Represents a user in the system",
    statusCode = 200
)
@JsonSchema
class User {
    val name: String = ""
}
```

Generates:

```json
{
  "type": "object",
  "title": "User Entity",
  "description": "Represents a user in the system",
  "statusCode": 200,
  "properties": {
    "name": { "type": "string" }
  }
}
```

## Combining with Standard Annotations

Custom annotations work alongside standard annotations:

```kotlin
@OpenApiDescription("The user entity")
@Description(
    title = "User",
    description = "System user",
    statusCode = 200
)
@JsonSchema
class User {
    @OpenApiRequired
    @OpenApiExample("John Doe")
    val name: String = ""
}
```
