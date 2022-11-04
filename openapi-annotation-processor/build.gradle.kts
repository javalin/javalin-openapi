description = "Javalin OpenAPI Annotation Processor | Generates OpenApi specification from @OpenApi annotations"

dependencies {
    api(project(":openapi-specification"))

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.20")
    implementation("com.google.code.gson:gson:2.10")
    implementation("io.swagger.parser.v3:swagger-parser:2.0.32")

    val logback = "1.4.3"
    implementation("ch.qos.logback:logback-core:$logback")
    implementation("ch.qos.logback:logback-classic:$logback")
    implementation("org.slf4j:slf4j-api:1.7.36")
}