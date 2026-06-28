package io.javalin.openapi.dynamic;

import io.javalin.openapi.OneOf;

public class Shape {

    @OneOf({ Dog.class, Cat.class })
    public Object getAnimal() {
        return null;
    }

    /** Composition with no explicit refs — reflection cannot discover subtypes, so this must NOT emit `oneOf: []`. */
    @OneOf({})
    public Object getEmpty() {
        return null;
    }
}
