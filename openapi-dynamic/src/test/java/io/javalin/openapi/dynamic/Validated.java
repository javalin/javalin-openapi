package io.javalin.openapi.dynamic;

import io.javalin.openapi.OpenApiNumberValidation;
import io.javalin.openapi.OpenApiPropertyType;

public class Validated {

    @OpenApiNumberValidation(minimum = "1", maximum = "10")
    public int getScore() {
        return 0;
    }

    @OpenApiPropertyType(definedBy = String.class)
    public int getRedirected() {
        return 0;
    }
}
