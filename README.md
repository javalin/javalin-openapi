# Javalin OpenAPI [![CI](https://github.com/dzikoysk/javalin-openapi/actions/workflows/gradle.yml/badge.svg)](https://github.com/dzikoysk/javalin-openapi/actions/workflows/gradle.yml)
Experimental compile-time OpenAPI integration for Javalin ecosystem

![Preview](https://user-images.githubusercontent.com/4235722/122982162-d2344f80-d39a-11eb-9a93-e52b9b7b7b53.png)

**Notes**
* Supports Java 8+ (also 16 and any further releases) and Kotlin (through [Kapt](https://kotlinlang.org/docs/kapt.html))
* Reflection free, does not perform any extra operations at runtime
* Uses `@OpenApi` to simplify migration from bundled OpenApi implementation
* Uses internal WebJar handler that works with `/*` route out of the box
* Provides better projection of OpenAPI specification
* Annotation processor uses Gradle to simplify workflow
* Does not support ReDoc
    
**Structure**
* `openapi-processor` - compile-time annotation processor, should generate `openapi.json` resource or just a class
* `openapi-api` - annotations used by annotation processor to generate OpenAPI docs

Javalin:

* `openapi-javalin-plugin` - loads `openapi.json` and serves openapi endpoint and swagger frontend
* `openapi-javalin-apptest` - application that uses OpenApi plugin

### Setup

Download required dependencies:

```groovy
repositories {
    maven { url 'https://repo.panda-lang.org/releases' }
}

dependencies {
    annotationProcessor "com.dzikoysk:openapi-processor:1.0.0" // Use Kapt in Kotlin projects 
    implementation "com.dzikoysk:openapi-annotations:1.0.0"
    implementation "com.dzikoysk:openapi-javalin-plugin:1.0.0"
}
```

And enable OpenAPI plugin:

```java
Javalin.create(config -> {
    OpenApiConfiguration openApiConfiguration = new OpenApiConfiguration();
    openApiConfiguration.setDocumentationPath("/swagger-docs");
    config.registerPlugin(new OpenApiPlugin(openApiConfiguration));
})
.start(80);
```

### Used by
* [Reposilite](https://github.com/dzikoysk/reposilite)
