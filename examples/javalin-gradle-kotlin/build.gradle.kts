sourceSets.getByName("main") {
    java.srcDir("src/main/kotlin")
}

dependencies {
    // declare lombok annotation processor as first
    val lombok =  "1.18.24"
    compileOnly("org.projectlombok:lombok:$lombok")
    annotationProcessor("org.projectlombok:lombok:$lombok")
    testCompileOnly("org.projectlombok:lombok:$lombok")
    testAnnotationProcessor("org.projectlombok:lombok:$lombok")

    // then openapi annotation processor
    annotationProcessor(project(":openapi-annotation-processor"))
    implementation(project(":javalin-plugins:javalin-openapi-plugin"))
    implementation(project(":javalin-plugins:javalin-swagger-plugin"))
    implementation(project(":javalin-plugins:javalin-redoc-plugin"))

    implementation("io.javalin:javalin:5.1.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0-rc1")

    val logback = "1.4.3"
    implementation("ch.qos.logback:logback-core:$logback")
    implementation("ch.qos.logback:logback-classic:$logback")
    implementation("org.slf4j:slf4j-api:2.0.3")
}