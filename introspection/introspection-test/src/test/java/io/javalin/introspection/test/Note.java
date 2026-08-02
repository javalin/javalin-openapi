package io.javalin.introspection.test;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Repeatable(Notes.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Note {
    String value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface Notes {
    Note[] value();
}

@Note("a")
@Note("b")
class Noted {}
