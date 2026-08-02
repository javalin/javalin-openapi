package io.javalin.openapi.dynamic;

import io.javalin.openapi.OpenApiByFields;

@OpenApiByFields
public class FieldsDto {

    public String publicField = "";
    private String privateField = "";
    public static String STATIC_FIELD = "";
}
