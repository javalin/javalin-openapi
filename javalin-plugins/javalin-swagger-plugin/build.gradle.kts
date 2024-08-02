description = "Javalin Swagger Plugin | Serve Swagger UI for OpenAPI specification"

dependencies {
    api(project(":openapi-specification"))
    @Suppress("GradlePackageUpdate")
    api("org.webjars:swagger-ui:5.17.14") // also bump swagger-ui version in OpenApiConfiguration
}
