package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;

public final class OpenApiResponseInstance extends AnnotationMirrorMapper {

    public OpenApiResponseInstance(AnnotationMirror mirror) {
        super(mirror);
    }

    public OpenApiContentInstance[] content() {
        return getArray("content", AnnotationMirror.class, OpenApiContentInstance::new, OpenApiContentInstance[]::new);
    }

    public String description() {
        return getString("description");
    }

    public String status() {
        return getString("status");
    }

    public Class<? extends Annotation> annotationType() {
        return OpenApiResponse.class;
    }

}
