description = "Javalin OpenAPI Dynamic Hook | Runtime OpenAPI generation hook for the OpenApiPlugin (route-based)"

dependencies {
    api(project(":openapi-dynamic"))
    api(project(":javalin-plugins:javalin-openapi-plugin"))
    compileOnly(libs.javalin)

    testImplementation(project(":javalin-plugins:javalin-redoc-plugin"))
    testImplementation(project(":javalin-plugins:javalin-swagger-plugin"))
    testImplementation(libs.javalin)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
    testImplementation(libs.unirest)
    testImplementation(libs.logback.classic)
}
