import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("kapt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        freeCompilerArgs.addAll("-Xjvm-default=all")
    }
}

sourceSets.getByName("main") {
    java.srcDir("src/main/kotlin")
}

dependencies {
    // declare lombok annotation processor as first
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    implementation(libs.jakarta.validation.api)

    // then openapi annotation processor
    kapt(project(":openapi-annotation-processor"))
    implementation(project(":javalin-plugins:javalin-openapi-plugin"))
    implementation(project(":javalin-plugins:javalin-swagger-plugin"))
    implementation(project(":javalin-plugins:javalin-redoc-plugin"))

    // javalin
    implementation(libs.javalin)
    implementation(libs.jackson.databind)

    // logging
    implementation(libs.logback.classic)

    // some test integrations
    implementation(libs.mongodb.bson)
    testImplementation(libs.groovy)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
    testImplementation(libs.json.unit.assertj)
    testImplementation(libs.unirest)
    testImplementation(libs.logback.classic)
}

kapt {
    arguments {
        arg("openapi.info.title", "Awesome App")
        arg("openapi.info.version", "1.0.0")
    }
}

repositories {
    mavenCentral()
}
