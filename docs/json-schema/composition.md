# Type Composition

Use `@OneOf`, `@AnyOf`, and `@AllOf` to describe polymorphic types and type unions. These annotations work on both properties and classes.

## OneOf

Exactly one of the listed types must match:

```kotlin
@JsonSchema
class Configuration {
    @OneOf([FileStorage::class, S3Storage::class])
    val storage: Storage? = null
}
```

Generates:

```json
{
  "properties": {
    "storage": {
      "oneOf": [
        {
          "type": "object",
          "properties": { "path": { "type": "string" } }
        },
        {
          "type": "object",
          "properties": { "bucket": { "type": "string" } }
        }
      ]
    }
  }
}
```

In JSON Schema mode, referenced types are inlined. In OpenAPI mode, they use `$ref`.

## AnyOf

One or more of the listed types may match:

```kotlin
@AnyOf([Email::class, Phone::class])
val contact: ContactMethod? = null
```

## AllOf

All of the listed types must match (used for inheritance / composition):

```kotlin
@AllOf([BaseEntity::class, UserFields::class])
val user: User? = null
```

## Class-level Composition

Apply composition annotations on classes directly:

```kotlin
@OneOf([Cat::class, Dog::class])
interface Animal
```

## Discriminator

Add a discriminator to distinguish between types:

```kotlin
@OneOf(
    value = [
        FileStorage::class,
        S3Storage::class
    ],
    discriminator = Discriminator(
        property = DiscriminatorProperty(
            name = "type",
            type = String::class
        ),
        mapping = [
            MappedClass(
                value = FileStorage::class,
                name = "filesystem"
            ),
            MappedClass(
                value = S3Storage::class,
                name = "s3"
            )
        ]
    )
)
val storage: Storage? = null
```

### Inject Discriminator Property

Set `injectInMappings = true` to automatically add the discriminator property to each mapped type:

```kotlin
@DiscriminatorProperty(
    name = "type",
    type = String::class,
    injectInMappings = true
)
```

### Discriminator Mapping Name

Use `@DiscriminatorMappingName` on a class to set its discriminator value:

```kotlin
@DiscriminatorMappingName("filesystem")
class FileStorage {
    val path: String = ""
}
```
