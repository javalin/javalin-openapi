# Compile-time Configuration

Configure the annotation processor using a Groovy script. This allows custom type mappings, property filters, and advanced type processors.

## Setup

Create a Groovy configuration script (e.g. `openapi.groovy`) anywhere in your project
and point the annotation processor to it using the `openapi.groovy.path` option:

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

=== "Gradle (Kotlin)"

    ```kotlin
    kapt {
        arguments {
            arg(
                "openapi.groovy.path",
                "$projectDir/src/main/compile/openapi.groovy"
            )
        }
    }
    ```

=== "Gradle (Groovy)"

    ```groovy
    kapt {
        arguments {
            arg(
                'openapi.groovy.path',
                "$projectDir/src/main/compile/openapi.groovy"
            )
        }
    }
    ```

=== "Maven"

    ```xml
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

## Annotation Processor Options

The following options can be passed to the annotation processor:

| Option                  | Description                                              |
|-------------------------|----------------------------------------------------------|
| `openapi.info.title`    | Set the `info.title` field in the generated specification |
| `openapi.info.version`  | Set the `info.version` field in the generated specification |
| `openapi.groovy.path`   | Path to the Groovy configuration script                   |

=== "Gradle (Kapt)"

    ```kotlin
    kapt {
        arguments {
            arg("openapi.info.title", "My API")
            arg("openapi.info.version", "1.0.0")
        }
    }
    ```

=== "Maven"

    ```xml
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
            <compilerArgs>
                <arg>-Aopenapi.info.title=My API</arg>
                <arg>-Aopenapi.info.version=1.0.0</arg>
            </compilerArgs>
        </configuration>
    </plugin>
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
