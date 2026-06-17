description = "Introspection KSP | Kotlin Symbol Processing TypeIntrospector backend"

dependencies {
    api(project(":introspection:introspection-api"))
    implementation(libs.ksp.symbol.processing.api)

    testImplementation(project(":introspection:introspection-runtime"))
    testImplementation(libs.kctfork.core)
    testImplementation(libs.kctfork.ksp)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
}
