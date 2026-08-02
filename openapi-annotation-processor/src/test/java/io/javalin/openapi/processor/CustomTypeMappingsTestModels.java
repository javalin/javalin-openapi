package io.javalin.openapi.processor;

import io.javalin.openapi.OpenApiPropertyType;
import java.time.Instant;

class PrimitiveRedirectDto {
    @OpenApiPropertyType(definedBy = long.class)
    public Instant getCreatedAt() {
        return Instant.EPOCH;
    }
}
