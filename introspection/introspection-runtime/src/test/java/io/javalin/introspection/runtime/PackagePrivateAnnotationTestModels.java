package io.javalin.introspection.runtime;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface PackagePrivateAnnotation {
    String value();
}

@PackagePrivateAnnotation("package-private")
class PackagePrivateAnnotated {
}
