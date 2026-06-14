package io.javalin.openapi.dynamic.fixtures;

import io.javalin.openapi.OpenApiByFields;

@OpenApiByFields
public class TransientDto {
    public String kept = "";
    public transient String skipped = "";
}
