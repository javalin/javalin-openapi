# Static Configuration

Pass key-value options to the backend at build time. The `info.*` options apply to both the annotation processor (APT/Kapt) and KSP; `openapi.groovy.path` is APT/Kapt-only.

## Options

| Option                 | Description                                                                                          |
|------------------------|------------------------------------------------------------------------------------------------------|
| `openapi.info.title`   | Set the `info.title` field in the generated specification                                            |
| `openapi.info.version` | Set the `info.version` field in the generated specification                                          |
| `openapi.groovy.path`  | Path to a Groovy script for advanced configuration (APT/Kapt only, see [Scripting Configuration](./scripting)) |

::: code-group

```kotlin [Gradle (Kapt)]
kapt {
    arguments {
        arg("openapi.info.title", "My API")
        arg("openapi.info.version", "1.0.0")
    }
}
```

```kotlin [Gradle (KSP)]
ksp {
    arg("openapi.info.title", "My API")
    arg("openapi.info.version", "1.0.0")
}
```

```xml [Maven]
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

:::

For custom type mappings, property filters, and custom type processors, see [Scripting Configuration](./scripting).
