import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("kapt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
        languageVersion = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=all") // For generating default methods in interfaces
    }
}

sourceSets.getByName("main") {
    java.srcDir("src/main/kotlin")
}

dependencies {
    // declare lombok annotation processor as first
    val lombok = "1.18.28"
    compileOnly("org.projectlombok:lombok:$lombok")
    annotationProcessor("org.projectlombok:lombok:$lombok")
    testCompileOnly("org.projectlombok:lombok:$lombok")
    testAnnotationProcessor("org.projectlombok:lombok:$lombok")
    implementation("jakarta.validation:jakarta.validation-api:2.0.2")

    // then openapi annotation processor
    kapt(project(":openapi-annotation-processor"))
    implementation(project(":javalin-plugins:javalin-openapi-plugin"))
    implementation(project(":javalin-plugins:javalin-swagger-plugin"))
    implementation(project(":javalin-plugins:javalin-redoc-plugin"))
    testImplementation("org.apache.groovy:groovy:4.0.12")

    // javalin
    implementation("io.javalin:javalin:6.1.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    // logging
    implementation("ch.qos.logback:logback-classic:1.4.13")

    // some test integrations
    implementation("org.mongodb:bson:4.9.1")
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
