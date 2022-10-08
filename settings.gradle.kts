rootProject.name = "javalin-openapi"

include(
    "openapi-specification",
    "openapi-annotation-processor",
    "javalin-plugins",
    "javalin-plugins:javalin-openapi-plugin",
    "javalin-plugins:javalin-swagger-plugin",
    "javalin-plugins:javalin-redoc-plugin",
    "examples",
    "examples:javalin-gradle-kotlin"
)