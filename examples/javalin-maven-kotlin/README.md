# javalin-maven-kotlin

This is a simple example of a Javalin application using Maven and Kotlin. 
In order to generate OpenAPI specification with Maven, run the following command:

```shell
$ mvn clean compile
```

Once the command is executed, the OpenAPI annotation processor will generate output files in the `target/classes/openapi-plugin` directory.
These files will be picked up by the OpenAPI plugin and hosted at `http://localhost:8080/openapi`.
You can also access the Swagger UI at `http://localhost:8080/swagger-ui`.
