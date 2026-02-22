description = "Javalin ReDoc Plugin | Serve ReDoc UI for OpenAPI specification"

dependencies {
    api(project(":openapi-specification"))

    implementation("org.webjars.npm:redoc:2.5.0") { // also bump redoc-ui version in OpenApiConfiguration
        exclude(group = "org.webjars.npm")
    }
    implementation("org.webjars.npm:js-tokens:8.0.3")
}
