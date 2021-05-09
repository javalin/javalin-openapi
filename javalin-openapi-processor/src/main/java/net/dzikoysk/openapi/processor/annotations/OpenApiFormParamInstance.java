package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.OpenApiFormParam;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;

public final class OpenApiFormParamInstance extends AnnotationMirrorMapper {

    OpenApiFormParamInstance(AnnotationMirror mirror) {
        super(mirror);
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
        return OpenApiFormParam.class;
    }

}
