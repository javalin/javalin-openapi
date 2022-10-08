description = "Javalin ReDoc Plugin | Serve ReDoc UI for OpenAPI specification"

dependencies {
    api(project(":openapi-specification"))
    api("org.webjars.npm:redoc:2.0.0-rc.70") // also bump redoc-ui version in OpenApiConfiguration
    api("org.webjars.npm:js-tokens:5.0.0")
}