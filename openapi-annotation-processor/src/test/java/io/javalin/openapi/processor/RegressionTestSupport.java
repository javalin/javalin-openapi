package io.javalin.openapi.processor;

import io.javalin.openapi.CustomAnnotation;
import io.javalin.openapi.OpenApiName;
import io.javalin.openapi.OpenApiPropertyType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;

class PrimitiveRedirectDto {
    @OpenApiPropertyType(definedBy = long.class)
    public Instant getCreatedAt() {
        return Instant.EPOCH;
    }
}

class FluentOpenApiNameDto {
    @OpenApiName("age")
    public int age() {
        return 1;
    }
}

@Inherited
@CustomAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface InheritedExtra {
    String inherited();
}

@InheritedExtra(inherited = "yes")
class InheritedExtraBase {
}

class InheritedExtraChild extends InheritedExtraBase {
    public String getName() {
        return "";
    }
}

record RecordWithExtraGetter(String id) {
    public String getDisplayName() {
        return "";
    }
}

@CustomAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface NestedExtra {
    NestedValue nested();
}

@Retention(RetentionPolicy.RUNTIME)
@interface NestedValue {
    String note();
}

@NestedExtra(nested = @NestedValue(note = "x"))
class NestedExtraDto {
    public String getName() {
        return "";
    }
}
