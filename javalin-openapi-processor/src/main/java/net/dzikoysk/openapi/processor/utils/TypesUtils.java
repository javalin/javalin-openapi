package net.dzikoysk.openapi.processor.utils;

import net.dzikoysk.openapi.processor.OpenApiAnnotationProcessor;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public final class TypesUtils {

    public static void getType(TypeMirror typeMirror) {
        Element element = OpenApiAnnotationProcessor.getTypes().asElement(typeMirror);
        List<? extends Element> enclosedElements = element.getEnclosedElements();
    }



}
