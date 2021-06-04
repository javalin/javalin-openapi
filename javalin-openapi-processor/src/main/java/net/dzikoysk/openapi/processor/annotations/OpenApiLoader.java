package net.dzikoysk.openapi.processor.annotations;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public final class OpenApiLoader {

    public static Collection<OpenApiInstance> loadAnnotations(Set<? extends TypeElement> annotations, Elements elements, Types types, RoundEnvironment roundEnv) {
        Collection<OpenApiInstance> openApiAnnotations = new ArrayList<>();

        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

            for (Element annotatedElement : annotatedElements) {
                AnnotationMirror openApiAnnotationMirror = annotatedElement.getAnnotationMirrors().get(0);
                OpenApiInstance openApiInstance = new OpenApiInstance(openApiAnnotationMirror);
                openApiAnnotations.add(openApiInstance);
                // types.asElement(openApiInstance.responses()[0].content()[0].from()).getEnclosedElements()
            }
        }

        return openApiAnnotations;
    }

}
