rootProject.name = "javalin-openapi"

include(
    "openapi-specification",
    "openapi-generator",
    "openapi-annotation-processor",
    "openapi-dynamic",
    "openapi-ksp",
    "introspection",
    "introspection:introspection-api",
    "introspection:introspection-runtime",
    "introspection:introspection-jap",
    "introspection:introspection-ksp",
    "introspection:introspection-test",
    "javalin-plugins",
    "javalin-plugins:javalin-openapi-plugin",
    "javalin-plugins:javalin-swagger-plugin",
    "javalin-plugins:javalin-redoc-plugin",
    "javalin-plugins:javalin-openapi-dynamic-hook",
    "examples",
    "examples:javalin-gradle-kotlin"
)