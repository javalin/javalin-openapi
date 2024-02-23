import org.jetbrains.kotlin.gradle.tasks.KaptGenerateStubs

description = "Javalin OpenAPI Annotation Processor | Generates OpenApi specification from @OpenApi annotations"

plugins {
    kotlin("kapt")
}

dependencies {
    api(project(":openapi-specification"))
    kaptTest(project(":openapi-annotation-processor"))
    testImplementation(project(":openapi-annotation-processor"))

    implementation(kotlin("reflect"))
    implementation("org.apache.groovy:groovy:4.0.9")

    implementation("io.javalin:javalin:6.1.1") {
        exclude(group = "org.slf4j")
    }

    implementation("io.swagger.parser.v3:swagger-parser:2.1.15")

    implementation("ch.qos.logback:logback-classic:1.4.13")

    testImplementation("org.mongodb:bson:4.9.0")
}

tasks.withType<KaptGenerateStubs> {
    dependsOn(
        ":openapi-annotation-processor:clean",
    )
}
