description = "Javalin OpenAPI Specification | Compile-time OpenAPI integration for Javalin 7.x"

dependencies {
    compileOnly(libs.javalin)
    api(libs.jackson.annotations)

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
