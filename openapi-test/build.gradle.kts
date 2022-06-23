dependencies {
    annotationProcessor(project(":openapi-annotation-processor"))
    implementation(project(":javalin-openapi-plugin"))
    implementation(project(":javalin-swagger-plugin"))
    implementation(project(":javalin-redoc-plugin"))
    implementation("io.javalin:javalin:5.0.0-SNAPSHOT")

    val logback = "1.3.0-alpha16"
    implementation("ch.qos.logback:logback-core:$logback")
    implementation("ch.qos.logback:logback-classic:$logback")
    implementation("org.slf4j:slf4j-api:2.0.0-alpha7")
}