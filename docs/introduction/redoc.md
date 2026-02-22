# Javalin ReDoc

The `ReDocPlugin` serves [ReDoc](https://github.com/Redocly/redoc) for clean, readable API reference documentation. Register it alongside the `OpenApiPlugin` to get a hosted ReDoc page at `/redoc`:

```kotlin
config.registerPlugin(ReDocPlugin())
```

## Configuration

Pass a lambda to customize the plugin. The most common options are the documentation source path and the UI route:

```kotlin
config.registerPlugin(ReDocPlugin { redoc ->
    redoc.documentationPath = "/openapi"  // where the OpenAPI JSON is served
    redoc.uiPath = "/redoc"               // where ReDoc is mounted
    redoc.title = "API Reference"         // HTML page title
})
```

All available options:

| Property | Default | Description |
|----------|---------|-------------|
| `documentationPath` | `/openapi` | Path to the OpenAPI JSON endpoint |
| `uiPath` | `/redoc` | Route where ReDoc is served |
| `title` | `"OpenApi documentation"` | HTML page title |
| `version` | `"2.5.0"` | ReDoc bundle version |
| `basePath` | `""` | Base path prefix for reverse proxy setups |
| `roles` | `[]` | Access control roles |
| `webJarPath` | `/webjars/redoc` | WebJar serving location |

When running behind a reverse proxy that adds a base path, set `basePath` so the UI loads assets correctly:

```kotlin
redoc.basePath = "/api"
```

`ReDocPlugin` is repeatable — you can register multiple instances for different configurations.
