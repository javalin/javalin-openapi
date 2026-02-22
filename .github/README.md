# OpenAPI Plugin [![CI](https://github.com/javalin/javalin-openapi/actions/workflows/gradle.yml/badge.svg)](https://github.com/javalin/javalin-openapi/actions/workflows/gradle.yml) ![Maven Central](https://img.shields.io/maven-central/v/io.javalin.community.openapi/openapi-annotation-processor?label=Maven%20Central) [![Version / Snapshot](https://maven.reposilite.com/api/badge/latest/snapshots/io/javalin/community/openapi/javalin-openapi-plugin?color=A97BFF&name=Snapshot)](https://maven.reposilite.com/#/snapshots/io/javalin/community/openapi)
Compile-time OpenAPI integration for Javalin 7.x ecosystem.
This is a new plugin that replaces [old built-in OpenApi module](https://github.com/javalin/javalin/tree/javalin-4x/javalin-openapi), 
the API looks quite the same despite some minor changes.

![Preview](https://user-images.githubusercontent.com/4235722/122982162-d2344f80-d39a-11eb-9a93-e52b9b7b7b53.png)

### How to use

* [Wiki / Installation](https://github.com/javalin/javalin-openapi/wiki/1.-Installation)
* [Wiki / Setup](https://github.com/javalin/javalin-openapi/wiki/2.-Setup)
* [Wiki / Features](https://github.com/javalin/javalin-openapi/wiki/3.-Features)

### Notes
* Reflection free
* Uses `@OpenApi` to simplify migration from bundled OpenApi implementation
* Supports Java 17+ and Kotlin (through [Kapt](https://kotlinlang.org/docs/kapt.html))
* Uses internal WebJar handler that works with `/*` route out of the box
* Provides better projection of OpenAPI specification
* Schema validation through Swagger core module

### Other examples
* [Test module](https://github.com/javalin/javalin-openapi/blob/main/examples/javalin-gradle-kotlin/src/main/java/io/javalin/openapi/plugin/test/JavalinTest.java) - `JavalinTest` shows how this plugin work in Java codebase using various features
* [Reposilite](https://github.com/dzikoysk/reposilite) - real world app using Javalin and OpenApi integration

### Repository structure

#### Universal modules

| Module                         | Description                                                                                |
|:-------------------------------|:-------------------------------------------------------------------------------------------|
| `openapi-annotation-processor` | Compile-time annotation processor, should generate `/openapi-plugin/openapi-{default/<version>}.json` resources |
| `openapi-generator`            | Shared schema generation logic: type mappings, runtime builder DSL, example generation     |
| `openapi-specification`        | Annotations & classes used to describe OpenAPI specification                               |
| `examples`                 | Example Javalin application that uses OpenApi plugin in Gradle & Maven                     |

#### Javalin plugins

| Plugin                   | Description                                                                    |
|:-------------------------|:-------------------------------------------------------------------------------|
| `javalin-openapi-plugin` | Loads `/openapi-plugin/openapi-{default/<version>}.json` resources and serves main OpenApi endpoint |
| `javalin-swagger-plugin` | Serves Swagger UI                                                              |
| `javalin-redoc-plugin`   | Serves ReDoc UI                                                                |

#### Branches

| Branch                                                       | Javalin version | OpenApi Version | Java Version |
|:-------------------------------------------------------------|:----------------|:----------------|:-------------|
| [main](https://github.com/javalin/javalin-openapi/tree/main) | 7.x             | 7.x             | JDK17        |
| [6.x](https://github.com/javalin/javalin-openapi/tree/6.x)   | 6.x             | 6.x             | JDK11        |
| [5.x](https://github.com/javalin/javalin-openapi/tree/5.x)   | 5.x             | 5.x             | JDK11        |
| [4.x](https://github.com/javalin/javalin-openapi/tree/4.x)   | 4.x             | 1.x             | JDK8         |
