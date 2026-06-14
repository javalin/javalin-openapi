dependencies {
    testImplementation(project(":introspection:introspection-api"))
    testImplementation(project(":introspection:introspection-runtime"))
    testImplementation(project(":introspection:introspection-jap"))

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
}
