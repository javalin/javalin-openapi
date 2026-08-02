package io.javalin.openapi.dynamic;

import io.javalin.openapi.OpenApiName;

public enum Role {
    ADMIN,
    @OpenApiName("regular_user") USER
}
