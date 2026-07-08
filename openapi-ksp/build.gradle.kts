description = "Javalin OpenAPI KSP | Kotlin Symbol Processing backend for OpenAPI schema generation (experimental)"

dependencies {
    api(project(":openapi-generator"))
    api(project(":introspection:introspection-ksp"))
    api(libs.ksp.symbol.processing.api) // OpenApiSymbolProcessorProvider/Processor are public KSP SymbolProcessor types (ServiceLoader entry point)

    testImplementation(libs.kctfork.core)
    testImplementation(libs.kctfork.ksp)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
}
