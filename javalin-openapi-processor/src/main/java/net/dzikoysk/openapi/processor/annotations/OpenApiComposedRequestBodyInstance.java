package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.OpenApiComposedRequestBody;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
final class OpenApiComposedRequestBodyInstance extends AnnotationMirrorMapper implements OpenApiComposedRequestBody {

    public OpenApiComposedRequestBodyInstance(AnnotationMirror mirror) {
        super(mirror);
    }

    @Override
    public OpenApiContent[] anyOf() {
        return getArray("anyOf", AnnotationMirror.class, OpenApiContentInstance::new, OpenApiContent[]::new);
    }

    @Override
    public String contentType() {
        return getString("contentType");
    }

    @Override
    public String description() {
        return getString("description");
    }

    @Override
    public OpenApiContent[] oneOf() {
        return getArray("oneOf", AnnotationMirror.class, OpenApiContentInstance::new, OpenApiContent[]::new);
    }

    @Override
    public boolean required() {
        return getBoolean("required");
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return OpenApiComposedRequestBody.class;
    }

}
