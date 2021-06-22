# Javalin OpenAPI
Experimental compile-time OpenAPI integration for Javalin ecosystem

**Notes**
* Supports Java 8+ (also 16 and any further releases) and Kotlin (through [Kapt](https://kotlinlang.org/docs/kapt.html))
* Reflection free, does not perform any extra operations at runtime
* Uses `@OpenApi` to simplify migration from bundled OpenApi implementation
* Uses internal WebJar handler that works with `/*` route out of the box
* Provides better projection of OpenAPI specification
* Annotation processor uses Gradle to simplify workflow
    
**Structure**
* `openapi-processor` - compile-time annotation processor, should generate `openapi.json` resource or just a class
* `openapi-api` - annotations used by annotation processor to generate OpenAPI docs

Javalin:

* `openapi-javalin-plugin` - loads `openapi.json` and serves openapi endpoint and swagger frontend
* `openapi-javalin-apptest` - application that uses OpenApi plugin
