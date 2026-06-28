package io.javalin.openapi.dynamic;

import io.javalin.openapi.OpenApiNaming;
import io.javalin.openapi.OpenApiNamingStrategy;

@OpenApiNaming(OpenApiNamingStrategy.SNAKE_CASE)
public class SnakeCaseDto {

    public String getFirstName() {
        return "";
    }
}
