package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
final class OpenApiRequestBodyInstance extends AnnotationMirrorMapper implements OpenApiRequestBody {

    public OpenApiRequestBodyInstance(AnnotationMirror mirror) {
        super(mirror);
    }

    @Override
    public OpenApiContent[] content() {
        return getArray("content", AnnotationMirror.class, OpenApiContentInstance::new, OpenApiContent[]::new);
    }

    @Override
    public String description() {
        return getString("description");
    }

    @Override
    public boolean required() {
        return getBoolean("required");
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return OpenApiRequestBody.class;
    }

}
