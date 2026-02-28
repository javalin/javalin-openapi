description = "Javalin OpenAPI Generator | JSON schema generation for OpenAPI documents"

dependencies {
    compileOnly(libs.javalin)
    api(project(":openapi-specification"))
    api(libs.jackson.databind)
    api(libs.jackson.module.kotlin)

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
