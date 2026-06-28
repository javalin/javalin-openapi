# Scripting Configuration

For advanced configuration - custom type mappings, property filters, and custom type processors - the annotation processor can run a Groovy script at compile time.

::: warning Experimental, and intentionally temporary
Scripting (`@ExperimentalCompileOpenApiConfiguration`) is a stop-gap gateway that exists only because there are no built-in static options for these advanced cases yet. It is **experimental**: its API may change, and the goal is to fold its capabilities into proper [static configuration](./configuration) over time, so this scripting layer may shrink or fade away in future releases. Prefer static options where they exist; reach for scripting only when you must.

It also applies to the **APT/Kapt backend only** - it is not loaded by the KSP backend.
:::

## Setup

Create a Groovy configuration script (e.g. `openapi.groovy`) anywhere in your project
and point the annotation processor at it with the `openapi.groovy.path` option:

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

::: code-group

```kotlin [Gradle (Kotlin)]
kapt {
    arguments {
        arg(
            "openapi.groovy.path",
            "$projectDir/src/main/compile/openapi.groovy"
        )
    }
}
```

```groovy [Gradle (Groovy)]
kapt {
    arguments {
        arg(
            'openapi.groovy.path',
            "$projectDir/src/main/compile/openapi.groovy"
        )
    }
}
```

```xml [Maven]
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <compilerArgs>
            <arg>-Aopenapi.groovy.path=${project.basedir}/src/main/compile/openapi.groovy</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

:::

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

Insert custom logic for handling specific types (e.g., unwrapping `AtomicReference<T>`):

```groovy
configuration.insertEmbeddedTypeProcessor({
    EmbeddedTypeProcessorContext context ->
        if (context.type.simpleName == 'AtomicReference'
            && context.type.generics.size() == 1) {
            context.parentContext.typeSchemaGenerator.addType(
                context.scheme,
                context.type.generics[0],
                context.inlineRefs,
                context.references,
                false
            )
            return true // handled
        }

        return false // use default processing
})
```

Custom type processors run before all built-in type processing, so they can override the default behavior for any type.

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

## Migrating your `openapi.groovy`

The scripting API renamed two types. If your script references the schema type or its mirror accessor - typically inside `propertyInSchemeFilter` - update these names:

| Before                    | After                 |
|---------------------------|-----------------------|
| `ClassDefinition`         | `OpenApiType`         |
| `ClassDefinitionHandleKt` | `OpenApiTypeHandleKt` |

```groovy
// before
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.ClassDefinitionHandleKt

configuration.propertyInSchemeFilter = { ctx, ClassDefinition type, property ->
    ctx.isAssignable(ClassDefinitionHandleKt.getMirror(type), ...)
}

// after
import io.javalin.openapi.experimental.OpenApiType
import io.javalin.openapi.experimental.OpenApiTypeHandleKt

configuration.propertyInSchemeFilter = { ctx, OpenApiType type, property ->
    ctx.isAssignable(OpenApiTypeHandleKt.getMirror(type), ...)
}
```

Everything else is unchanged: `simpleTypeMappings`, `insertEmbeddedTypeProcessor`, `debug`, `validateWithParser`, and the `OpenApiAnnotationProcessorConfiguration`/`OpenApiAnnotationProcessorConfigurer`/`SimpleType`/`EmbeddedTypeProcessorContext` types all keep their names and members. Scripts that don't name `ClassDefinition`/`ClassDefinitionHandleKt` need no changes.
