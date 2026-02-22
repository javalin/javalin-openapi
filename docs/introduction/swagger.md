# Javalin Swagger UI

The `SwaggerPlugin` serves [Swagger UI](https://swagger.io/tools/swagger-ui/) for interactive API exploration. Register it alongside the `OpenApiPlugin` to get a browser-based endpoint explorer at `/swagger`:

```kotlin
config.registerPlugin(SwaggerPlugin())
```

## Configuration

Pass a lambda to customize the plugin. The most common options are the documentation source path and the UI route:

```kotlin
config.registerPlugin(SwaggerPlugin { swagger ->
    swagger.documentationPath = "/openapi"  // where the OpenAPI JSON is served
    swagger.uiPath = "/swagger"             // where Swagger UI is mounted
    swagger.title = "My API Documentation"  // HTML page title
})
```

All available options:

| Property            | Default                                    | Description                               |
|---------------------|--------------------------------------------|-------------------------------------------|
| `documentationPath` | `/openapi`                                 | Path to the OpenAPI JSON endpoint         |
| `uiPath`            | `/swagger`                                 | Route where Swagger UI is served          |
| `title`             | `"OpenApi documentation"`                  | HTML page title                           |
| `version`           | `"5.17.14"`                                | Swagger UI bundle version                 |
| `basePath`          | `""`                                       | Base path prefix for reverse proxy setups |
| `roles`             | `[]`                                       | Access control roles                      |
| `validatorUrl`      | `"https://validator.swagger.io/validator"` | Validator endpoint URL                    |
| `tagsSorter`        | `"'alpha'"`                                | Tag sorting strategy                      |
| `operationsSorter`  | `"'alpha'"`                                | Operation sorting strategy                |
| `webJarPath`        | `/webjars/swagger-ui`                      | WebJar serving location                   |

## Version Switching

If your API serves multiple specification versions, you can populate the Swagger UI version dropdown:

```kotlin
swagger.injectCustomVersion("v1", "/openapi?v=v1")
swagger.injectCustomVersion("v2", "/openapi?v=v2")
```

## Customization

You can inject custom stylesheets and scripts to theme or extend the UI:

```kotlin
swagger.injectStylesheet("/custom/swagger-theme.css")
swagger.injectJavaScript("/custom/swagger-init.js")
```

When running behind a reverse proxy that adds a base path, set `basePath` so the UI loads assets correctly:

```kotlin
swagger.basePath = "/api"
```

`SwaggerPlugin` is repeatable — you can register multiple instances for different API versions or configurations.
