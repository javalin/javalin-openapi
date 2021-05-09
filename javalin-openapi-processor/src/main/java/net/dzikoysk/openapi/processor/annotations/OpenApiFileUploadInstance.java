package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.OpenApiFileUpload;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;

public final class OpenApiFileUploadInstance extends AnnotationMirrorMapper {

    public OpenApiFileUploadInstance(AnnotationMirror mirror) {
        super(mirror);
    }

    public String description() {
        return getString("description");
    }

    public boolean isArray() {
        return getBoolean("isArray");
    }

    public String name() {
        return getString("name");
    }

    public boolean required() {
        return getBoolean("required");
    }

    public Class<? extends Annotation> annotationType() {
        return OpenApiFileUpload.class;
    }

}
