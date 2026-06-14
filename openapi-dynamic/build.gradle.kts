description = "Javalin OpenAPI Dynamic | Runtime reflection-based OpenAPI introspection (experimental)"

dependencies {
    api(project(":openapi-generator"))
    api(project(":introspection:introspection-runtime"))

    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
}
