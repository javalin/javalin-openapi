description = "Javalin OpenAPI Specification | Compile-time OpenAPI integration for Javalin 6.x"

dependencies {
    val jacksonVersion = "2.15.2"
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    api("com.google.code.gson:gson:2.10.1")
}