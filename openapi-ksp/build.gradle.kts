description = "Javalin OpenAPI KSP | Kotlin Symbol Processing backend for OpenAPI schema generation (experimental)"

dependencies {
    api(project(":openapi-generator"))
    api(project(":introspection:introspection-ksp"))
    api(libs.ksp.symbol.processing.api)

    testImplementation(libs.kctfork.core)
    testImplementation(libs.kctfork.ksp)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
}
