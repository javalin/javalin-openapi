description = "Javalin OpenAPI Annotation Processor | Generates OpenApi specification from @OpenApi annotations"

dependencies {
    api(project(":openapi-specification"))

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.20")
    implementation("io.javalin:javalin:5.1.3")
    implementation("com.google.code.gson:gson:2.10")
    implementation("io.swagger.parser.v3:swagger-parser:2.1.7")

    val logback = "1.4.4"
    implementation("ch.qos.logback:logback-core:$logback")
    implementation("ch.qos.logback:logback-classic:$logback")
    implementation("org.slf4j:slf4j-api:1.7.36")
}