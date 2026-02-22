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
    implementation("org.apache.groovy:groovy:4.0.30")

    implementation("io.javalin:javalin:7.0.0") {
        exclude(group = "org.slf4j")
    }

    implementation("io.swagger.parser.v3:swagger-parser:2.1.38")
    implementation("ch.qos.logback:logback-classic:1.5.32")

    testImplementation("org.mongodb:bson:5.6.3")
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
