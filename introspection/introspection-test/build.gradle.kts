dependencies {
    testImplementation(project(":introspection:introspection-api"))
    testImplementation(project(":introspection:introspection-runtime"))
    testImplementation(project(":introspection:introspection-jap"))
    testImplementation(project(":introspection:introspection-ksp"))
    testImplementation(libs.ksp.symbol.processing.api)
    testImplementation(libs.kctfork.core)
    testImplementation(libs.kctfork.ksp)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
}
