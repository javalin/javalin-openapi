package io.javalin.openapi.dynamic;

import io.javalin.openapi.OneOf;

public class Shape {

    @OneOf({ Dog.class, Cat.class })
    public Object getAnimal() {
        return null;
    }

    @OneOf({})
    public Object getEmpty() {
        return null;
    }
}
