dependencies {
    implementation(project(":openapi-annotations"))
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("io.swagger.parser.v3:swagger-parser:2.0.32")

    val logback = "1.2.11"
    implementation("ch.qos.logback:logback-core:$logback")
    implementation("ch.qos.logback:logback-classic:$logback")
    implementation("org.slf4j:slf4j-api:1.7.36")
}