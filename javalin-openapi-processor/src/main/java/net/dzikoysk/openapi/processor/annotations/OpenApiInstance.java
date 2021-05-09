package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import java.lang.annotation.Annotation;

public final class OpenApiInstance extends AnnotationMirrorMapper {

    public OpenApiInstance(AnnotationMirror mirror) {
        super(mirror);
    }

    public OpenApiComposedRequestBodyInstance composedRequestBody() {
        return getAnnotation("composedRequestBody", OpenApiComposedRequestBodyInstance::new);
    }

    public OpenApiParamInstance[] cookies() {
        return getArray("cookies", AnnotationMirror.class, OpenApiParamInstance::new, OpenApiParamInstance[]::new);
    }

    public boolean deprecated() {
        return getBoolean("deprecated");
    }

    public String description() {
        return getString("description");
    }

    public OpenApiFileUploadInstance[] fileUploads() {
        return getArray("fileUploads", AnnotationMirror.class, OpenApiFileUploadInstance::new, OpenApiFileUploadInstance[]::new);
    }

    public OpenApiFormParamInstance[] formParams() {
        return getArray("formParams", AnnotationMirror.class, OpenApiFormParamInstance::new, OpenApiFormParamInstance[]::new);
    }

    public OpenApiParamInstance[] headers() {
        return getArray("headers", AnnotationMirror.class, OpenApiParamInstance::new, OpenApiParamInstance[]::new);
    }

    public boolean ignore() {
        return getBoolean("ignore");
    }

    public HttpMethod method() {
        return HttpMethod.valueOf(getValue("method").toString().toUpperCase());
    }

    public String operationId() {
        return getString("operationId");
    }

    public String path() {
        return getString("path");
    }

    public OpenApiParamInstance[] pathParams() {
        return getArray("pathParams", AnnotationMirror.class, OpenApiParamInstance::new, OpenApiParamInstance[]::new);
    }

    public OpenApiParamInstance[] queryParams() {
        return getArray("queryParams", AnnotationMirror.class, OpenApiParamInstance::new, OpenApiParamInstance[]::new);
    }

    public OpenApiRequestBodyInstance requestBody() {
        return getAnnotation("requestBody", OpenApiRequestBodyInstance::new);
    }

    public OpenApiResponseInstance[] responses() {
        return getArray("responses", AnnotationMirror.class).stream()
                .map(OpenApiResponseInstance::new)
                .toArray(OpenApiResponseInstance[]::new);
    }

    public OpenApiSecurityInstance[] security() {
        return getArray("security", AnnotationMirror.class, OpenApiSecurityInstance::new, OpenApiSecurityInstance[]::new);
    }

    public String summary() {
        return getString("summary");
    }

    public String[] tags() {
        return getArray("tags", AnnotationValue.class).stream()
                .map(value -> value.getValue().toString())
                .toArray(String[]::new);
    }

    public Class<? extends Annotation> annotationType() {
        return OpenApi.class;
    }

}
