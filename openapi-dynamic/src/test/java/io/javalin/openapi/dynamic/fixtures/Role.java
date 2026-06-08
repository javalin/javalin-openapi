package io.javalin.openapi.dynamic.fixtures;

import io.javalin.openapi.OpenApiName;

public enum Role {
    ADMIN,
    @OpenApiName("regular_user") USER
}
