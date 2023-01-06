import org.jetbrains.kotlin.gradle.tasks.KaptGenerateStubs

description = "Javalin OpenAPI Annotation Processor | Generates OpenApi specification from @OpenApi annotations"

plugins {
    kotlin("kapt")
}

dependencies {
    api(project(":openapi-specification"))
    kaptTest(project(":openapi-annotation-processor"))
    testImplementation(project(":openapi-annotation-processor"))

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.22")
    implementation("org.apache.groovy:groovy:4.0.7")

    implementation("io.javalin:javalin:5.3.0") {
        exclude(group = "org.slf4j")
    }
    implementation("io.swagger.parser.v3:swagger-parser:2.1.8")

    @Suppress("GradlePackageUpdate")
    implementation("ch.qos.logback:logback-classic:1.2.11")

    testImplementation("org.mongodb:bson:4.8.1")
}

tasks.withType<KaptGenerateStubs> {
    dependsOn(
        ":openapi-annotation-processor:clean",
    )
}