package io.javalin.openapi.dynamic;

import io.javalin.openapi.OpenApiName;

public class FluentOpenApiNameDto {

    @OpenApiName("age")
    public int age() {
        return 1;
    }
}
