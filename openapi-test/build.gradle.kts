dependencies {
    annotationProcessor(project(":openapi-annotation-processor"))
    implementation(project(":javalin-openapi-plugin"))
    implementation(project(":javalin-swagger-plugin"))
    implementation(project(":javalin-redoc-plugin"))
    implementation("io.javalin:javalin:4.6.3")

    val logback = "1.2.5"
    implementation("ch.qos.logback:logback-core:$logback")
    implementation("ch.qos.logback:logback-classic:$logback")
    implementation("org.slf4j:slf4j-api:1.7.32")
}