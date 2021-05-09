package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.OpenApiRequestBody;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;

public final class OpenApiRequestBodyInstance extends AnnotationMirrorMapper {

    public OpenApiRequestBodyInstance(AnnotationMirror mirror) {
        super(mirror);
    }

    public OpenApiContentInstance[] content() {
        return getArray("content", AnnotationMirror.class, OpenApiContentInstance::new, OpenApiContentInstance[]::new);
    }

    public String description() {
        return getString("description");
    }

    public boolean required() {
        return getBoolean("required");
    }

    public Class<? extends Annotation> annotationType() {
        return OpenApiRequestBody.class;
    }

}
