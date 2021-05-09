package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.OpenApiSecurity;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
final class OpenApiSecurityInstance extends AnnotationMirrorMapper implements OpenApiSecurity {

    public OpenApiSecurityInstance(AnnotationMirror mirror) {
        super(mirror);
    }

    @Override
    public String name() {
        return getString("name");
    }

    @Override
    public String[] scopes() {
        return getArray("scopes", AnnotationValue.class, value -> value.getValue().toString(), String[]::new);
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return OpenApiSecurity.class;
    }

}
