plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    ksp(project(":openapi-ksp"))
    implementation(project(":javalin-plugins:javalin-openapi-plugin"))
    implementation(project(":javalin-plugins:javalin-swagger-plugin"))
    implementation(project(":javalin-plugins:javalin-redoc-plugin"))

    implementation(libs.javalin)
    implementation(libs.jackson.databind)
    implementation(libs.logback.classic)
}

ksp {
    arg("openapi.info.title", "Awesome KSP App")
    arg("openapi.info.version", "1.0.0")
}

application {
    mainClass.set("io.javalin.openapi.ksp.example.AppKt")
}

repositories {
    mavenCentral()
}
