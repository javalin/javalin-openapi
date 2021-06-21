package net.dzikoysk.openapi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
public @interface OpenApiPropertyType {

    Class<?> definedBy();

}
