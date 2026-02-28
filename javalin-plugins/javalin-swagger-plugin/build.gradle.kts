description = "Javalin Swagger Plugin | Serve Swagger UI for OpenAPI specification"

dependencies {
    compileOnly(libs.javalin)
    api(project(":openapi-specification"))
    @Suppress("GradlePackageUpdate")
    api(libs.swagger.ui) // also bump swagger-ui version in OpenApiConfiguration

    testImplementation(libs.javalin)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
    testImplementation(libs.json.unit.assertj)
    testImplementation(libs.unirest)
    testImplementation(libs.logback.classic)
}
