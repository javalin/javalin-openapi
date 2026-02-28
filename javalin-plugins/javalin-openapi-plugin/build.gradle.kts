description = "Javalin OpenAPI Plugin | Serve raw OpenApi documentation under dedicated endpoint"

plugins {
    kotlin("kapt")
}

dependencies {
    compileOnly(libs.javalin)
    api(project(":openapi-generator"))

    kaptTest(project(":openapi-annotation-processor"))

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
