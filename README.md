# OpenAPI Annotation Processor [![CI](https://github.com/dzikoysk/javalin-openapi/actions/workflows/gradle.yml/badge.svg)](https://github.com/dzikoysk/javalin-openapi/actions/workflows/gradle.yml)
Experimental compile-time OpenAPI integration for Javalin and Ktor ecosystem

![Preview](https://user-images.githubusercontent.com/4235722/122982162-d2344f80-d39a-11eb-9a93-e52b9b7b7b53.png)

**Notes**
* Supports Java 8+ (also 16 and any further releases) and Kotlin (through [Kapt](https://kotlinlang.org/docs/kapt.html))
* Reflection free, does not perform any extra operations at runtime
* Uses `@OpenApi` to simplify migration from bundled OpenApi implementation
* Uses internal WebJar handler that works with `/*` route out of the box
* Provides better projection of OpenAPI specification **#TODO**
    
**Structure**
* `openapi-annotation-processor` - compile-time annotation processor, should generate `openapi.json` resource or just a class
* `openapi-annotations` - annotations used by annotation processor to generate OpenAPI docs

Javalin:

* `javalin-openapi-plugin` - loads `openapi.json` resource and serves OpenApi endpoint
* `javalin-swagger-plugin` - serves Swagger UI
* `javalin-redoc-plugin` - serves ReDoc UI
* `javalin-apptest` - example Javalin application that uses OpenApi plugin


Ktor:

* `ktor-openapi-plugin` - loads `openapi.json` resource and serves OpenApi endpoint
* `ktor-apptest` - example Ktor application that uses OpenApi plugin


### Setup

Download required dependencies:

```groovy
repositories {
    maven { url 'https://repo.panda-lang.org/releases' }
}

dependencies {
    def openapi = "1.0.8"
    annotationProcessor "com.dzikoysk:openapi-annotation-processor:$openapi" // Use Kapt in Kotlin projects 
    
    // Javalin
    implementation "com.dzikoysk:javalin-openapi-plugin:$openapi"
    implementation "com.dzikoysk:javalin-swagger-plugin:$openapi" // for Swagger UI
    implementation "com.dzikoysk:javalin-redoc-plugin:$openapi" // for ReDoc UI
    
    // Ktor
    implementation "com.dzikoysk:ktor-openapi-plugin:$openapi"
}
```

And enable OpenAPI plugin for Javalin with Swagger UI:

```java
Javalin.create(config -> {
    String deprecatedDocsPath = "/swagger-docs";
    
    OpenApiConfiguration openApiConfiguration = new OpenApiConfiguration();
    openApiConfiguration.setTitle("AwesomeApp");
    openApiConfiguration.setDocumentationPath(deprecatedDocsPath); // by default it's /openapi
    config.registerPlugin(new OpenApiPlugin(openApiConfiguration));
    
    SwaggerConfiguration swaggerConfiguration = new SwaggerConfiguration();
    swaggerConfiguration.setDocumentationPath(deprecatedDocsPath);
    config.registerPlugin(new SwaggerPlugin(swaggerConfiguration));
    
    ReDocConfiguration reDocConfiguration = new ReDocConfiguration();
    reDocConfiguration.setDocumentationPath(deprecatedDocsPath);
    config.registerPlugin(new ReDocPlugin(reDocConfiguration));
})
.start(80);
```

Or for Ktor application using the features:

```kotlin
install(OpenApiFeature) {
    documentationPath = "/swagger-docs"
}

// Swagger and ReDoc are not supported yet
```

### Used by
* [Reposilite](https://github.com/dzikoysk/reposilite) with Javalin
* [Hub](https://github.com/panda-lang/hub) with Ktor