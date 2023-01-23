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
        languageVersion = "1.7"
        freeCompilerArgs = listOf("-Xjvm-default=all") // For generating default methods in interfaces
    }
}

sourceSets.getByName("main") {
    java.srcDir("src/main/kotlin")
}

dependencies {
    // declare lombok annotation processor as first
    val lombok = "1.18.24"
    compileOnly("org.projectlombok:lombok:$lombok")
    annotationProcessor("org.projectlombok:lombok:$lombok")
    testCompileOnly("org.projectlombok:lombok:$lombok")
    testAnnotationProcessor("org.projectlombok:lombok:$lombok")

    // then openapi annotation processor
    kapt(project(":openapi-annotation-processor"))
    implementation(project(":javalin-plugins:javalin-openapi-plugin"))
    implementation(project(":javalin-plugins:javalin-swagger-plugin"))
    implementation(project(":javalin-plugins:javalin-redoc-plugin"))
    testImplementation("org.apache.groovy:groovy:4.0.7")

    // javalin
    implementation("io.javalin:javalin:5.3.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0")

    // logging
    implementation("ch.qos.logback:logback-classic:1.4.4")

    // some test integrations
    implementation("org.mongodb:bson:4.7.2")
}
repositories {
    mavenCentral()
}
