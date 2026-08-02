package io.javalin.openapi.processor;

import io.javalin.openapi.OpenApiName;

class FluentOpenApiNameDto {
    @OpenApiName("age")
    public int age() {
        return 1;
    }
}

record RecordWithExtraGetter(String id) {
    public String getDisplayName() {
        return "";
    }
}
