package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.OpenApiFileUpload;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
final class OpenApiFileUploadInstance extends AnnotationMirrorMapper implements OpenApiFileUpload {

    public OpenApiFileUploadInstance(AnnotationMirror mirror) {
        super(mirror);
    }

    @Override
    public String description() {
        return getString("description");
    }

    @Override
    public boolean isArray() {
        return getBoolean("isArray");
    }

    @Override
    public String name() {
        return getString("name");
    }

    @Override
    public boolean required() {
        return getBoolean("required");
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return OpenApiFileUpload.class;
    }

}
