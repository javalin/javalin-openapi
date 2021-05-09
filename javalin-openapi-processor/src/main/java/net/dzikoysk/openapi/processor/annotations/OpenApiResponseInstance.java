package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
final class OpenApiResponseInstance extends AnnotationMirrorMapper implements OpenApiResponse {

    public OpenApiResponseInstance(AnnotationMirror mirror) {
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
    public String status() {
        return getString("status");
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return OpenApiResponse.class;
    }

}
