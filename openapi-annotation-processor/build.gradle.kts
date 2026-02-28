import org.jetbrains.kotlin.gradle.tasks.KaptGenerateStubs

description = "Javalin OpenAPI Annotation Processor | Generates OpenApi specification from @OpenApi annotations"

plugins {
    kotlin("kapt")
}

dependencies {
    api(project(":openapi-generator"))
    kaptTest(project(":openapi-annotation-processor"))
    testImplementation(project(":openapi-annotation-processor"))

    implementation(kotlin("reflect"))
    implementation(libs.groovy)

    implementation(libs.javalin) {
        exclude(group = "org.slf4j")
    }

    implementation(libs.swagger.parser) {
        exclude(group = "com.fasterxml.jackson")
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "com.fasterxml.jackson.dataformat")
        exclude(group = "com.fasterxml.jackson.datatype")
    }
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.datatype.jsr310)

    implementation(libs.logback.classic)

    testImplementation(libs.javalin)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
    testImplementation(libs.json.unit.assertj)
    testImplementation(libs.unirest)
    testImplementation(libs.logback.classic)
    testImplementation(libs.mongodb.bson)
}

kapt {
    arguments {
        arg("openapi.groovy.path", "$projectDir/src/test/compile/openapi.groovy")
    }
}

tasks.withType<KaptGenerateStubs> {
    dependsOn(
        ":openapi-annotation-processor:clean",
    )
}
