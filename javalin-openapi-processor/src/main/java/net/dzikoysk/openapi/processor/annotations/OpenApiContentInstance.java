package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.OpenApiContent;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
final class OpenApiContentInstance extends AnnotationMirrorMapper implements OpenApiContent {

    public OpenApiContentInstance(AnnotationMirror mirror) {
        super(mirror);
    }

    @Override
    public Class<?> from() {
        return null;
    }

    @Override
    public boolean isArray() {
        return getBoolean("isArray");
    }

    @Override
    public String type() {
        return getString("type");
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return OpenApiContent.class;
    }

}
