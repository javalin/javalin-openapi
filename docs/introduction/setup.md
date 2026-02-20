# Setup with Javalin

Javalin OpenAPI is a compile-time annotation processor that generates OpenAPI 3.0.3 specifications and JSON Schema Draft-7 documents. It works with any Java/Kotlin project, but provides first-class integration with the [Javalin](https://javalin.io) web framework through plugins that serve the generated specification and host Swagger UI or ReDoc.

This page covers the Javalin integration. If you're using a different framework or want standalone schema generation, see [Setup without Javalin](./json-schema-setup).

## Requirements

- Java 17+
- Javalin 7.x

## Installation

::: code-group

```kotlin [Gradle (Kotlin)]
repositories {
    mavenCentral()
}

dependencies {
    val openapi = "7.0.0-beta.3"

    annotationProcessor(
        "io.javalin.community.openapi:openapi-annotation-processor:$openapi"
    )
    implementation(
        "io.javalin.community.openapi:javalin-openapi-plugin:$openapi"
    )
    // Optional: Swagger UI
    implementation(
        "io.javalin.community.openapi:javalin-swagger-plugin:$openapi"
    )
    // Optional: ReDoc
    implementation(
        "io.javalin.community.openapi:javalin-redoc-plugin:$openapi"
    )
}
```

```kotlin [Gradle (Kotlin) with Kapt]
plugins {
    kotlin("kapt")
}

dependencies {
    val openapi = "7.0.0-beta.3"

    kapt(
        "io.javalin.community.openapi:openapi-annotation-processor:$openapi"
    )
    implementation(
        "io.javalin.community.openapi:javalin-openapi-plugin:$openapi"
    )
    implementation(
        "io.javalin.community.openapi:javalin-swagger-plugin:$openapi"
    )
    implementation(
        "io.javalin.community.openapi:javalin-redoc-plugin:$openapi"
    )
}
```

```xml [Maven]
<dependencies>
    <dependency>
        <groupId>io.javalin.community.openapi</groupId>
        <artifactId>javalin-openapi-plugin</artifactId>
        <version>7.0.0-beta.3</version>
    </dependency>
    <!-- Optional: Swagger UI -->
    <dependency>
        <groupId>io.javalin.community.openapi</groupId>
        <artifactId>javalin-swagger-plugin</artifactId>
        <version>7.0.0-beta.3</version>
    </dependency>
    <!-- Optional: ReDoc -->
    <dependency>
        <groupId>io.javalin.community.openapi</groupId>
        <artifactId>javalin-redoc-plugin</artifactId>
        <version>7.0.0-beta.3</version>
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
                        <version>7.0.0-beta.3</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

:::

## Register the Plugin

```kotlin
Javalin.create { config ->
    config.registerPlugin(OpenApiPlugin { openapi ->
        openapi.withDefinitionConfiguration { _, builder ->
            builder.info { info ->
                info.title = "My API"
            }
        }
    })
}.start(8080)
```

This serves the generated OpenAPI JSON at `/openapi`.

### API Info

```kotlin
builder.info { info ->
    info.title = "My API"
    info.description = "API description"
    info.termsOfService = "https://example.com/tos"
    info.setContact { contact ->
        contact.name = "API Support"
        contact.url = "https://example.com/support"
        contact.email = "support@example.com"
    }
    info.setLicense { license ->
        license.name = "Apache 2.0"
        license.identifier = "Apache-2.0"
    }
}
```

### Servers

```kotlin
builder.server { server ->
    server.url = "https://api.example.com"
    server.description = "Production"
    server.addVariable(
        name = "version",
        defaultValue = "v1",
        enumValues = arrayOf("v1", "v2"),
        description = "API version"
    )
}
```

### Security Schemes

```kotlin
builder
    .withBearerAuth("BearerAuth")
    .withBasicAuth("BasicAuth")
    .withApiKeyAuth("ApiKeyAuth", "X-API-Key")
    .withCookieAuth("CookieAuth", "session_id")
    .withOpenID("OpenID", "https://example.com/.well-known/openid")
    .withOAuth2("OAuth2", "OAuth2 authentication") { oauth ->
        oauth.withImplicitFlow("https://example.com/auth") { implicit ->
            implicit.withScope("read:users", "Read users")
        }
    }
    .withGlobalSecurity("BearerAuth")
```

### Documentation Path & Access Control

```kotlin
config.registerPlugin(OpenApiPlugin { openapi ->
    openapi.withRoles(MyRoles.ADMIN)
    openapi.withDefinitionConfiguration { version, builder ->
        builder.info { info ->
            info.title = "My API - $version"
        }
    }
})
```

### Definition Processor

Post-process the generated OpenAPI JSON before serving:

```kotlin
openapi.withDefinitionProcessor { json ->
    // Modify the ObjectNode
    json.toPrettyString()
}
```

### Multiple Versions

Each version generates a separate OpenAPI specification. Configure them in `@OpenApi(versions = ...)` on your handlers and handle them in the plugin:

```kotlin
openapi.withDefinitionConfiguration { version, builder ->
    if (version == "v1") {
        builder.info { it.title = "API v1" }
    }
}
```

`OpenApiPlugin` is repeatable — you can register multiple instances for different configurations.

## Next Steps

- [Javalin Swagger UI](./swagger) — interactive API explorer
- [Javalin ReDoc](./redoc) — clean API reference
- [OpenAPI Getting Started](../openapi/getting-started) — annotate your first endpoint
