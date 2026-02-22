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
    val lombok = "1.18.42"
    compileOnly("org.projectlombok:lombok:$lombok")
    annotationProcessor("org.projectlombok:lombok:$lombok")
    testCompileOnly("org.projectlombok:lombok:$lombok")
    testAnnotationProcessor("org.projectlombok:lombok:$lombok")
    implementation("jakarta.validation:jakarta.validation-api:3.1.1")

    // then openapi annotation processor
    kapt(project(":openapi-annotation-processor"))
    implementation(project(":javalin-plugins:javalin-openapi-plugin"))
    implementation(project(":javalin-plugins:javalin-swagger-plugin"))
    implementation(project(":javalin-plugins:javalin-redoc-plugin"))
    testImplementation("org.apache.groovy:groovy:4.0.30")

    // javalin
    implementation("io.javalin:javalin:7.0.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.0")

    // logging
    implementation("ch.qos.logback:logback-classic:1.5.32")

    // some test integrations
    implementation("org.mongodb:bson:5.6.3")
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
