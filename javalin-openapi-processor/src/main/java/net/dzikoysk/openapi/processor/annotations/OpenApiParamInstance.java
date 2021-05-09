package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.OpenApiParam;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;

public final class OpenApiParamInstance extends AnnotationMirrorMapper {

    public OpenApiParamInstance(AnnotationMirror mirror) {
        super(mirror);
    }

    public boolean allowEmptyValue() {
        return getBoolean("allowEmptyValue");
    }

    public boolean deprecated() {
        return getBoolean("boolean");
    }

    public String description() {
        return getString("description");
    }

    public boolean isRepeatable() {
        return getBoolean("isRepeatable");
    }

    public String name() {
        return getString("name");
    }

    public boolean required() {
        return getBoolean("required");
    }

    public TypeMirror type() {
        return getType("type");
    }

    public Class<? extends Annotation> annotationType() {
        return OpenApiParam.class;
    }

}
