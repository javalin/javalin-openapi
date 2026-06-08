rootProject.name = "javalin-openapi"

include(
    "openapi-specification",
    "openapi-generator",
    "openapi-annotation-processor",
    "openapi-dynamic",
    "javalin-plugins",
    "javalin-plugins:javalin-openapi-plugin",
    "javalin-plugins:javalin-swagger-plugin",
    "javalin-plugins:javalin-redoc-plugin",
    "javalin-plugins:javalin-openapi-dynamic-hook",
    "examples",
    "examples:javalin-gradle-kotlin"
)