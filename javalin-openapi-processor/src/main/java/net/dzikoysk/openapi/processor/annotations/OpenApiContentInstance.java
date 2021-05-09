package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.OpenApiContent;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;

public final class OpenApiContentInstance extends AnnotationMirrorMapper {

    public OpenApiContentInstance(AnnotationMirror mirror) {
        super(mirror);
    }

    public TypeMirror from() {
        return getType("from");
    }

    public boolean isArray() {
        return getBoolean("isArray");
    }

    public String type() {
        return getString("type");
    }

    public Class<? extends Annotation> annotationType() {
        return OpenApiContent.class;
    }

}
