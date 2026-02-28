description = "Javalin ReDoc Plugin | Serve ReDoc UI for OpenAPI specification"

dependencies {
    compileOnly(libs.javalin)
    api(project(":openapi-specification"))

    implementation(libs.redoc) { // also bump redoc-ui version in OpenApiConfiguration
        exclude(group = "org.webjars.npm")
    }
    implementation(libs.js.tokens)

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
