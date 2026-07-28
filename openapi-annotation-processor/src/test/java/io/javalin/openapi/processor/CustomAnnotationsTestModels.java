package io.javalin.openapi.processor;

import io.javalin.openapi.CustomAnnotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
