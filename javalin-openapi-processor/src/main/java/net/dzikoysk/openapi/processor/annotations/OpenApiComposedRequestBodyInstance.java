package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.OpenApiComposedRequestBody;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;

public final class OpenApiComposedRequestBodyInstance extends AnnotationMirrorMapper {

    public OpenApiComposedRequestBodyInstance(AnnotationMirror mirror) {
        super(mirror);
    }

    public OpenApiContentInstance[] anyOf() {
        return getArray("anyOf", AnnotationMirror.class, OpenApiContentInstance::new, OpenApiContentInstance[]::new);
    }

    public String contentType() {
        return getString("contentType");
    }

    public String description() {
        return getString("description");
    }

    public OpenApiContentInstance[] oneOf() {
        return getArray("oneOf", AnnotationMirror.class, OpenApiContentInstance::new, OpenApiContentInstance[]::new);
    }

    public boolean required() {
        return getBoolean("required");
    }

    public Class<? extends Annotation> annotationType() {
        return OpenApiComposedRequestBody.class;
    }

}
