# OpenAPI Plugin [![CI](https://github.com/javalin/javalin-openapi/actions/workflows/gradle.yml/badge.svg)](https://github.com/javalin/javalin-openapi/actions/workflows/gradle.yml) ![Maven Central](https://img.shields.io/maven-central/v/io.javalin.community.openapi/openapi-annotation-processor?label=Maven%20Central) [![Version / Snapshot](https://maven.reposilite.com/api/badge/latest/snapshots/io/javalin/community/openapi/javalin-openapi-plugin?color=A97BFF&name=Snapshot)](https://maven.reposilite.com/#/snapshots/io/javalin/community/openapi)
Compile-time OpenAPI integration for Javalin 5.x ecosystem.
This is a new plugin that replaces [old built-in OpenApi module](https://github.com/javalin/javalin/tree/javalin-4x/javalin-openapi), 
the API looks quite the same despite some minor changes.

![Preview](https://user-images.githubusercontent.com/4235722/122982162-d2344f80-d39a-11eb-9a93-e52b9b7b7b53.png)

### How to use

* [Wiki / Installation](https://github.com/javalin/javalin-openapi/wiki/1.-Installation)
* [Wiki / Setup](https://github.com/javalin/javalin-openapi/wiki/2.-Setup)
* [Wiki / Features](https://github.com/javalin/javalin-openapi/wiki/3.-Features)

### Notes
* Reflection free, does not perform any extra operations at runtime
* Uses `@OpenApi` to simplify migration from bundled OpenApi implementation
* Supports Java 11+ (also 16 and any further releases) and Kotlin (through [Kapt](https://kotlinlang.org/docs/kapt.html))
* Uses internal WebJar handler that works with `/*` route out of the box
* Provides better projection of OpenAPI specification
* Schema validation through Swagger core module

### Other examples
* [Test module](https://github.com/javalin/javalin-openapi/blob/main/examples/javalin-gradle-kotlin/src/main/java/io/javalin/openapi/plugin/test/JavalinTest.java) - `JavalinTest` shows how this plugin work in Java codebase using various features
* [Reposilite](https://github.com/dzikoysk/reposilite) - real world app using Javalin and OpenApi integration

### Repository structure
* `openapi-annotation-processor` - compile-time annotation processor, should generate `/openapi-plugin/openapi.json` resource
* `openapi-specification` - annotations & classes used to describe OpenAPI specification
* `openapi-test` - example Javalin application that uses OpenApi plugin in Gradle & Maven

Javalin:

* `javalin-openapi-plugin` - loads `/openapi-plugin/openapi.json` resource and serves main OpenApi endpoint
* `javalin-swagger-plugin` - serves Swagger UI
* `javalin-redoc-plugin` - serves ReDoc UI
