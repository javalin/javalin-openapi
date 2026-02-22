description = "Javalin OpenAPI Specification | Compile-time OpenAPI integration for Javalin 7.x"

dependencies {
    val jacksonVersion = "2.21.0"
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
}