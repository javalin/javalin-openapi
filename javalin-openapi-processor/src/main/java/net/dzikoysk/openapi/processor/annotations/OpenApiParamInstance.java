package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.OpenApiParam;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
final class OpenApiParamInstance extends AnnotationMirrorMapper implements OpenApiParam {

    public OpenApiParamInstance(AnnotationMirror mirror) {
        super(mirror);
    }

    @Override
    public boolean allowEmptyValue() {
        return getBoolean("allowEmptyValue");
    }

    @Override
    public boolean deprecated() {
        return getBoolean("boolean");
    }

    @Override
    public String description() {
        return getString("description");
    }

    @Override
    public boolean isRepeatable() {
        return getBoolean("isRepeatable");
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
    public Class<?> type() {
        return null;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return OpenApiParam.class;
    }

}
