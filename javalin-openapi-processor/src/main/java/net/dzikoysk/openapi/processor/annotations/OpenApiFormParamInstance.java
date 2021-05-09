package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.OpenApiFormParam;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
final class OpenApiFormParamInstance extends AnnotationMirrorMapper implements OpenApiFormParam {

    OpenApiFormParamInstance(AnnotationMirror mirror) {
        super(mirror);
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
        return OpenApiFormParam.class;
    }

}
