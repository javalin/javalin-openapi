# Javalin OpenAPI
Experimental compile-time OpenAPI integration for Javalin ecosystem

**Notes**
* Supports Java 8+ (also 16)
* Reflection free, does not perform any extra operations at runtime
* Uses `@OpenApi` to maintain compatibility with builtin OpenAPI plugin
* Annotation processor development requires Gradle to simplify workflow
  * Of course the final plugin can be used also in Maven based projects 
    
**Structure**
* `javalin-openapi-apptest` - application that uses OpenApi plugin
* `javalin-openapi-plugin` - `openapi.json` loader, should serve openapi endpoint and swagger/redoc frontend
* `javalin-openapi-processor` - compile-time annotation processor, should generate `openapi.json` resource or just a class