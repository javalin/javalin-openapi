# Compile-time Configuration

Configure the annotation processor using a Groovy script. This allows custom type mappings, property filters, and advanced type processors.

## Setup

Create an `openapi.groovy` file at `src/main/compile/openapi.groovy`:

```groovy
import io.javalin.openapi.experimental.*

@ExperimentalCompileOpenApiConfiguration
class OpenApiConfiguration
    implements OpenApiAnnotationProcessorConfigurer {

    @Override
    void configure(
        OpenApiAnnotationProcessorConfiguration configuration
    ) {
        // Configuration goes here
    }
}
```

## Custom Type Mappings

Map custom types to simple OpenAPI types:

```groovy
void configure(
    OpenApiAnnotationProcessorConfiguration configuration
) {
    configuration.simpleTypeMappings[
        'org.bson.types.ObjectId'
    ] = new SimpleType("string")

    configuration.simpleTypeMappings[
        'com.example.CustomId'
    ] = new SimpleType(/* type */ "integer", /* format */ "int64")
}
```

## Property Filters

Control which properties are included in schemas:

```groovy
configuration.propertyInSchemeFilter = {
    ctx, type, property ->
        !property.simpleName
            .toString()
            .startsWith("internal")
}
```

## Custom Type Processors

Insert custom logic for handling specific types (e.g., unwrapping `Optional<T>`):

```groovy
configuration.insertEmbeddedTypeProcessor({
    EmbeddedTypeProcessorContext context ->
        def typeName = context.type.fullName

        if (typeName.startsWith("java.util.Optional")) {
            def innerType = context.type.generics[0]
            context.schema.addProperty("nullable", true)
            return context.createEmbeddedTypeDescription(
                innerType,
                context.inlineRefs
            )
        }

        return null // use default processing
})
```

## Debug Mode

Enable debug output during annotation processing:

```groovy
configuration.debug = true
```

## Parser Validation

Validate the generated specification with Swagger Parser:

```groovy
configuration.validateWithParser = true // default
```

## Full Example

```groovy
import io.javalin.openapi.experimental.*

@ExperimentalCompileOpenApiConfiguration
class OpenApiConfiguration
    implements OpenApiAnnotationProcessorConfigurer {

    @Override
    void configure(
        OpenApiAnnotationProcessorConfiguration configuration
    ) {
        configuration.simpleTypeMappings[
            'org.bson.types.ObjectId'
        ] = new SimpleType("string")

        configuration.simpleTypeMappings[
            'com.example.Money'
        ] = new SimpleType("string")

        configuration.propertyInSchemeFilter = {
            ctx, type, property ->
                !property.simpleName
                    .toString()
                    .startsWith("_")
        }

        configuration.debug = false
        configuration.validateWithParser = true
    }
}
```
