# Runtime Builder DSL

The `OpenApiSchemaBuilder` provides a Kotlin DSL for building and modifying OpenAPI specifications programmatically at runtime. Use it to extend compile-time generated schemas, build specs entirely in code, or combine both approaches.

## When to Use

- **Extend compile-time schemas** - add servers, security, or extra endpoints at startup
- **Dynamic endpoints** - describe routes registered at runtime that the annotation processor can't see
- **Testing** - build expected schemas in tests without JSON strings

## Basic Usage

```kotlin
val spec = OpenApiSchemaBuilder()
    .openApiVersion("3.1.0")
    .info { it.title("My API").version("1.0") }

spec.path("/users").operation("get") {
    tags("users")
    summary("List users")
    responses {
        response("200") {
            description("OK")
            content {
                mediaType("application/json") {
                    schema { ref("#/components/schemas/User") }
                }
            }
        }
    }
}

val json = spec.toJson()
```

## Schema DSL

The `SchemaBuilder` DSL provides a clean way to define inline schemas without working with raw JSON nodes. It supports the three most common patterns:

### Simple Type

```kotlin
schema { type("string") }
// → { "type": "string" }
```

### Type with Format

```kotlin
schema { type("integer"); format("int32") }
// → { "type": "integer", "format": "int32" }
```

### Reference

```kotlin
schema { ref("#/components/schemas/User") }
// → { "$ref": "#/components/schemas/User" }
```

The schema DSL is available on media types, parameters, headers, and object schema properties.

## Paths & Operations

```kotlin
spec.path("/users/{id}").operation("get") {
    tags("users")
    summary("Get user by ID")
    description("Returns a single user")
    operationId("getUserById")
    deprecated(false)
}
```

Multiple methods on the same path:

```kotlin
val path = spec.path("/users")
path.operation("get") { summary("List users") }
path.operation("post") { summary("Create user") }
```

## Parameters

Define parameters with a trailing schema lambda:

```kotlin
parameters {
    parameter("limit", "query") { type("integer") }
    parameter("offset", "query", description = "Starting position") {
        type("integer"); format("int32")
    }
}
```

Full parameter options:

```kotlin
parameter(
    name = "X-Request-ID",
    location = "header",
    description = "Tracking ID",
    required = true,
    deprecated = false,
    allowEmptyValue = false,
    example = "abc-123",
) { type("string") }
```

## Request Body

```kotlin
requestBody {
    description("User to create")
    required(true)
    content {
        mediaType("application/json") {
            schema { ref("#/components/schemas/User") }
        }
    }
}
```

## Responses

```kotlin
responses {
    response("200") {
        description("OK")
        content {
            mediaType("application/json") {
                schema { ref("#/components/schemas/User") }
                example("""{"name": "John"}""")
            }
        }
        headers {
            header("X-Request-Id") { type("string") }
            header("X-Rate-Limit", description = "Rate limit remaining") {
                type("integer")
            }
        }
    }
    response("404") { description("Not found") }
}
```

## Object Schemas

Build inline object schemas with properties:

```kotlin
mediaType("application/json") {
    objectSchema {
        property("name", "string", null)
        property("age", "integer", "int32")
        property("address") { ref("#/components/schemas/Address") }
        arrayProperty("tags") { ref("#/components/schemas/Tag") }
        arrayProperty("scores", "integer", "int32")
        additionalProperties { ref("#/components/schemas/Value") }
    }
}
```

## Callbacks

```kotlin
callbacks {
    callback("onEvent", "{request.body#/url}/callback", "post") {
        summary("Event callback")
        requestBody {
            content {
                mediaType("application/json") {
                    schema { ref("#/components/schemas/Event") }
                }
            }
        }
        responses {
            response("200") { description("OK") }
        }
    }
}
```

## Security

### Operation-level

```kotlin
spec.path("/secure").operation("get") {
    security {
        securityRequirement("BearerAuth")
        securityRequirement("OAuth2", "read", "write")
    }
}
```

### Global security and schemes

See [Setup with Javalin](../introduction/setup#security-schemes) for configuring security schemes and global security on the builder.

## Extending Compile-time Schemas

Load a compile-time generated spec and modify it at runtime:

```kotlin
val schema = OpenApiSchemaBuilder.fromJson(compileTimeJson)

// Add runtime configuration
schema.server { it.url("https://api.example.com") }
schema.withBearerAuth()

// Add security to an existing endpoint
schema.path("/users").operation("get") {
    security { securityRequirement("BearerAuth") }
}

val json = schema.toJson()
```

Reopening an existing operation preserves all fields - only the fields you set are changed. This makes it safe to layer runtime additions on top of compile-time output.

## Auto-generating Docs for Registered Routes

For a springdoc-style experience - documenting routes that are registered programmatically (and that the compile-time processor never sees) - use the **dynamic hook** module. It enumerates every route registered on Javalin at startup and contributes them to the served document.

```kotlin [Gradle (Kotlin)]
dependencies {
    val openapi = "7.2.2"
    implementation("io.javalin.community.openapi:javalin-openapi-dynamic-hook:$openapi")
}
```

Register `RegisteredRoutesHook` on the `OpenApiPlugin`:

```kotlin
Javalin.create { config ->
    config.registerPlugin(OpenApiPlugin { it.withHook(RegisteredRoutesHook()) })

    config.routes.get("/users") { /* ... */ }
    config.routes.get("/users/{id}") { /* ... */ }
}
```

Every registered route is documented with its path, method, path parameters (typed as `string`), and a default `200` response.

### Enriching a Route

Javalin handlers are opaque lambdas, so without extra information the hook can only emit those stubs - it cannot infer request/response bodies. Attach an `OpenApiMetadata` to a route to describe it using the same operation DSL shown above; `schema(Class)` resolves the type through the reflection schema engine (no annotation processing required):

```kotlin
config.routes.addEndpoint(
    Endpoint.create(HandlerType.GET, "/users/{id}")
        .addMetadata(OpenApiMetadata {
            summary("Get a user")
            responses {
                response("200") {
                    description("The user")
                    content { mediaType("application/json") { schema(User::class.java) } }
                }
            }
        })
        .handler { /* ... */ }
)
```

The path-parameter skeleton is still auto-added, then your metadata is overlaid on top, and referenced types (e.g. `User`) are emitted into `components/schemas`.

::: warning
The hook currently documents the `OpenApiPlugin`'s own `/openapi` route (and any Swagger/ReDoc UI routes) alongside your endpoints. Runtime reflection is opt-in: it only happens when you add this module and register the hook, so the compile-time backends stay reflection-free.
:::

## Merge Behavior

The builder supports incremental construction. Reopening a path, operation, response, or media type merges with existing data:

- **Operations**: preserve tags, summary, parameters, responses, etc. Only fields you explicitly set are overwritten
- **Parameters**: same name + location replaces; different name or location appends
- **Responses**: new status codes are added; same status code merges content and headers
- **Content types**: new media types are added; same media type merges schema and example
- **Tags**: `tags(...)` replaces; `addTag(...)` / `addTags(...)` appends
