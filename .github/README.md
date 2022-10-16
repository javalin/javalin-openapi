# OpenAPI Plugin [![CI](https://github.com/javalin/javalin-openapi/actions/workflows/gradle.yml/badge.svg)](https://github.com/javalin/javalin-openapi/actions/workflows/gradle.yml) ![Maven Central](https://img.shields.io/maven-central/v/io.javalin.community.openapi/openapi-annotation-processor?label=Maven%20Central) ![Version / Snapshot](https://maven.reposilite.com/api/badge/latest/snapshots/io/javalin/community/openapi/javalin-openapi-plugin?color=A97BFF&name=Snapshot)
Compile-time OpenAPI integration for Javalin 5.x ecosystem.
This is a new plugin that replaces [old built-in OpenApi module](https://github.com/javalin/javalin/tree/javalin-4x/javalin-openapi), 
the API looks quite the same despite some minor changes.

![Preview](https://user-images.githubusercontent.com/4235722/122982162-d2344f80-d39a-11eb-9a93-e52b9b7b7b53.png)

### Setup
Download required dependencies:

<details>
    <summary>Gradle setup for Javalin 5.x</summary>

```groovy
repositories {
    // For snapshots
    maven { url 'https://maven.reposilite.com/snapshots' }
}

dependencies {
    def openapi = "5.1.0"
    
    // For Java projects
    annotationProcessor("io.javalin.community.openapi:openapi-annotation-processor:$openapi")
    // For Kotlin projects
    kapt("io.javalin.community.openapi:openapi-annotation-processor:$openapi")

    implementation("io.javalin.community.openapi:javalin-openapi-plugin:$openapi") // for /openapi route with JSON scheme
    implementation("io.javalin.community.openapi:javalin-swagger-plugin:$openapi") // for Swagger UI
    implementation("io.javalin.community.openapi:javalin-redoc-plugin:$openapi") // for ReDoc UI
}
```

</details>

<details>
    <summary>Maven setup for Javalin 5.x</summary>

```xml
<project>
    <properties>
        <javalin.version>5.1.0</javalin.version>
    </properties>
    
    <repositories>
        <!-- Snapshots -->
        <repository>
            <id>reposilite-repository</id>
            <url>https://maven.reposilite.com/snapshots</url>
        </repository>
    </repositories>
    
    <dependencies>
        <!-- OpenApi plugin -->
        <dependency>
            <groupId>io.javalin.community.openapi</groupId>
            <artifactId>javalin-openapi-plugin</artifactId>
            <version>${javalin.version}</version>
        </dependency>
        <!-- Swagger plugin -->
        <dependency>
            <groupId>io.javalin.community.openapi</groupId>
            <artifactId>javalin-swagger-plugin</artifactId>
            <version>${javalin.version}</version>
        </dependency>
        <!-- ReDoc plugin -->
        <dependency>
            <groupId>io.javalin.community.openapi</groupId>
            <artifactId>javalin-redoc-plugin</artifactId>
            <version>${javalin.version}</version>
        </dependency>
        <dependency>
            <groupId>org.webjars.npm</groupId>
            <artifactId>redoc</artifactId>
            <version>2.0.0-rc.70</version>
            <exclusions>
                <exclusion>
                    <groupId>*</groupId>
                    <artifactId>*</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
    </dependencies>
    
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.10.1</version>
                    <configuration>
                        <annotationProcessorPaths>
                            <annotationProcessorPath>
                                <groupId>io.javalin.community.openapi</groupId>
                                <artifactId>openapi-annotation-processor</artifactId>
                                <version>${javalin.version}</version>
                            </annotationProcessorPath>
                        </annotationProcessorPaths>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

</details>

And enable OpenAPI plugin for Javalin with Swagger UI:

```java
Javalin.create(config -> {
    String deprecatedDocsPath = "/swagger-docs";
    
    OpenApiConfiguration openApiConfiguration = new OpenApiConfiguration();
    openApiConfiguration.getInfo().setTitle("AwesomeApp");
    openApiConfiguration.setDocumentationPath(deprecatedDocsPath); // by default it's /openapi
    config.plugins.register(new OpenApiPlugin(openApiConfiguration));
    
    SwaggerConfiguration swaggerConfiguration = new SwaggerConfiguration();
    swaggerConfiguration.setUiPath("/swagger"); // by default it's /swagger
    swaggerConfiguration.setDocumentationPath(deprecatedDocsPath);
    config.plugins.register(new SwaggerPlugin(swaggerConfiguration));
    
    ReDocConfiguration reDocConfiguration = new ReDocConfiguration();
    reDocConfiguration.setUiPath("/redoc"); // by default it's /redoc
    reDocConfiguration.setDocumentationPath(deprecatedDocsPath);
    config.plugins.register(new ReDocPlugin(reDocConfiguration));
})
.start(8080);
```

This plugin is also compatibile with Javalin 4.x, see: [Javalin RFC - OpenApi plugin](https://github.com/javalin/javalin-openapi/tree/99fe1f8eb1df46a1687653bf433d082d7115d426)

### Notes
* Reflection free, does not perform any extra operations at runtime
* Uses `@OpenApi` to simplify migration from bundled OpenApi implementation
* Supports Java 11+ (also 16 and any further releases) and Kotlin (through [Kapt](https://kotlinlang.org/docs/kapt.html))
* Uses internal WebJar handler that works with `/*` route out of the box
* Provides better projection of OpenAPI specification
* Schema validation through Swagger core module

### Other examples
* [Test module](https://github.com/javalin/javalin-openapi/blob/main/openapi-test/src/main/java/io/javalin/openapi/plugin/test/JavalinTest.java) - `JavalinTest` shows how this plugin work in Java codebase using various features
* [Reposilite](https://github.com/dzikoysk/reposilite) - real world app using Javalin and OpenApi integration
* [Javalin OpenApi Example](https://github.com/paulkagiri/JavalinOpenApiExample) by [paulkagiri](https://github.com/paulkagiri)

### Repository structure
* `openapi-annotation-processor` - compile-time annotation processor, should generate `/openapi-plugin/openapi.json` resource
* `openapi-specification` - annotations & classes used to describe OpenAPI specification
* `openapi-test` - example Javalin application that uses OpenApi plugin in Gradle & Maven

Javalin:

* `javalin-openapi-plugin` - loads `/openapi-plugin/openapi.json` resource and serves main OpenApi endpoint
* `javalin-swagger-plugin` - serves Swagger UI
* `javalin-redoc-plugin` - serves ReDoc UI
