description = "Introspection Runtime | Reflection-based TypeIntrospector backend"

dependencies {
    api(project(":introspection:introspection-api"))

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
}
