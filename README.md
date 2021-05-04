# Javalin OpenAPI
Experimental compile-time OpenAPI integration for Javalin ecosystem

**Notes**
* Supports Java 8+ (also 16)
* Reflection free, does not perform any extra operations at runtime
* Uses `@OpenApi` to maintain compatibility with builtin OpenAPI plugin
* Annotation processor development requires Gradle to simplify workflow
  * Of course the final plugin can be used also in Maven based projects 