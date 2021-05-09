package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.OpenApiSecurity;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import java.lang.annotation.Annotation;

public final class OpenApiSecurityInstance extends AnnotationMirrorMapper {

    public OpenApiSecurityInstance(AnnotationMirror mirror) {
        super(mirror);
    }

    public String name() {
        return getString("name");
    }

    public String[] scopes() {
        return getArray("scopes", AnnotationValue.class, value -> value.getValue().toString(), String[]::new);
    }

    public Class<? extends Annotation> annotationType() {
        return OpenApiSecurity.class;
    }

}
