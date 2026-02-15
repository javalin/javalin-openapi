---
layout: home

hero:
  name: Javalin OpenAPI
  text: Compile-time API Documentation
  tagline: Generate OpenAPI specifications and JSON Schemas at compile time with zero runtime reflection. Annotation-driven, type-safe, and ready for Swagger UI and ReDoc.
  actions:
    - theme: brand
      text: Get Started
      link: /introduction/setup
    - theme: alt
      text: View on GitHub
      link: https://github.com/javalin/javalin-openapi

features:
  - title: Compile-time Generation
    details: Schemas are generated during compilation using annotation processing. No runtime reflection, no classpath scanning, no startup overhead.
  - title: Two Modes
    details: Generate OpenAPI 3.0.3 endpoint documentation with @OpenApi, or standalone JSON Schema Draft-7 files with @JsonSchema — using the same annotation processor.
  - title: Swagger UI & ReDoc
    details: Built-in plugins serve Swagger UI and ReDoc out of the box. Register the plugin and your interactive API documentation is live.
  - title: Schema Customization
    details: Control property names with @OpenApiNaming, add examples with @OpenApiExample, override types with @OpenApiPropertyType, and validate with constraint annotations.
  - title: Enum Support
    details: Rename values with @OpenApiName, apply naming strategies with @OpenApiNaming, create integer enums with @OpenApiPropertyType, and add descriptions.
  - title: Compile-time Configuration
    details: Fine-tune the annotation processor with openapi.groovy — custom type mappings, property filters, and embedded type processors.
---
