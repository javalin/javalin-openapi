# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the Javalin OpenAPI plugin - a compile-time OpenAPI integration for Javalin 6.x. The plugin generates OpenAPI specifications from `@OpenApi` annotations without using reflection, performing all processing at compile time via annotation processors.

**Key characteristics:**
- Reflection-free, zero runtime overhead
- Supports Java 11+ and Kotlin (via KAPT)
- Generates `/openapi-plugin/openapi.json` during compilation
- Provides Swagger UI and ReDoc UI plugins

## Build Commands

```bash
# Build the entire project
./gradlew build

# Run tests
./gradlew test

# Build a specific module
./gradlew :openapi-annotation-processor:build
./gradlew :javalin-openapi-plugin:build

# Clean build artifacts
./gradlew clean

# Run tests for a specific module
./gradlew :openapi-annotation-processor:test

# Assemble jars without running tests
./gradlew assemble
```

## Module Architecture

The project is organized into three main categories:

### Core Modules

1. **openapi-specification** - Contains annotations and data classes that define the OpenAPI specification
   - Annotations: `@OpenApi`, `@JsonSchema`, `@OpenApiContent`, etc.
   - Used by both the annotation processor and runtime plugins
   - Dependencies: Jackson, Gson

2. **openapi-annotation-processor** - Compile-time annotation processor
   - Entry point: `OpenApiAnnotationProcessor.kt`
   - Generates OpenAPI JSON from annotated code during compilation
   - Key components:
     - `OpenApiGenerator` - Processes `@OpenApi` annotations
     - `JsonSchemaGenerator` - Processes `@JsonSchema` annotations
     - `OpenApiPrecompileScriptingEngine` - Supports Groovy-based configuration
   - Output: `/openapi-plugin/openapi.json` resource file

3. **javalin-plugins** - Runtime plugins for Javalin
   - `javalin-openapi-plugin` - Loads generated JSON and serves OpenAPI endpoint
   - `javalin-swagger-plugin` - Serves Swagger UI
   - `javalin-redoc-plugin` - Serves ReDoc UI

### Dependencies Between Modules

- Annotation processor depends on: specification module
- Javalin plugins depend on: specification module
- Example projects use KAPT to run the annotation processor at compile time

## Key Concepts

### Annotation Processing Flow

1. Developer annotates route handlers with `@OpenApi` and domain classes with `@JsonSchema`
2. During compilation, `OpenApiAnnotationProcessor` processes these annotations
3. Processor generates an OpenAPI specification JSON file as a resource
4. At runtime, `OpenApiPlugin` loads this pre-generated JSON and serves it

### Configuration

The annotation processor accepts compiler options:
- `kapt.arguments.openApiTitle` - OpenAPI info title
- `kapt.arguments.openApiVersion` - OpenAPI info version

Advanced configuration via Groovy scripts (see `OpenApiPrecompileScriptingEngine`).

## Testing

- Tests use JUnit 5
- Test dependencies include AssertJ and JSON-Unit for assertions
- KAPT is configured to run the annotation processor during test compilation
- Example applications in `examples/` directory serve as integration tests

## Publishing

- Publishes to Maven Central via Sonatype
- Also publishes snapshots to Reposilite (https://maven.reposilite.com)
- Version: 6.7.0-2 (see root `build.gradle.kts`)
- Signing configured for releases

## Important Implementation Details

- The `OpenApiAnnotationProcessor` must be stateless between compilation rounds
- Context is stored in `AnnotationProcessorContext` and reset each round
- The processor uses Java Compiler Tree API (`com.sun.source.util.Trees`) to access method bodies
- Generated schemas support composition, inheritance, and complex generic types
