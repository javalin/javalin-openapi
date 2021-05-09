package net.dzikoysk.openapi.processor.annotations;

import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiComposedRequestBody;
import io.javalin.plugin.openapi.annotations.OpenApiFileUpload;
import io.javalin.plugin.openapi.annotations.OpenApiFormParam;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import io.javalin.plugin.openapi.annotations.OpenApiSecurity;
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import java.lang.annotation.Annotation;
import java.util.Arrays;

@SuppressWarnings("ClassExplicitlyAnnotation")
final class OpenApiInstance extends AnnotationMirrorMapper implements OpenApi {

    public OpenApiInstance(AnnotationMirror mirror) {
        super(mirror);
    }

    @Override
    public OpenApiComposedRequestBody composedRequestBody() {
        return getAnnotation("composedRequestBody", OpenApiComposedRequestBodyInstance::new);
    }

    @Override
    public OpenApiParam[] cookies() {
        return getArray("cookies", AnnotationMirror.class, OpenApiParamInstance::new, OpenApiParam[]::new);
    }

    @Override
    public boolean deprecated() {
        return getBoolean("deprecated");
    }

    @Override
    public String description() {
        return getString("description");
    }

    @Override
    public OpenApiFileUpload[] fileUploads() {
        return getArray("fileUploads", AnnotationMirror.class, OpenApiFileUploadInstance::new, OpenApiFileUpload[]::new);
    }

    @Override
    public OpenApiFormParam[] formParams() {
        return getArray("formParams", AnnotationMirror.class, OpenApiFormParamInstance::new, OpenApiFormParam[]::new);
    }

    @Override
    public OpenApiParam[] headers() {
        return getArray("headers", AnnotationMirror.class, OpenApiParamInstance::new, OpenApiParam[]::new);
    }

    @Override
    public boolean ignore() {
        return getBoolean("ignore");
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.valueOf(getValue("method").toString().toUpperCase());
    }

    @Override
    public String operationId() {
        return getString("operationId");
    }

    @Override
    public String path() {
        return getString("path");
    }

    @Override
    public OpenApiParam[] pathParams() {
        return getArray("pathParams", AnnotationMirror.class, OpenApiParamInstance::new, OpenApiParam[]::new);
    }

    @Override
    public OpenApiParam[] queryParams() {
        return getArray("queryParams", AnnotationMirror.class, OpenApiParamInstance::new, OpenApiParam[]::new);
    }

    @Override
    public OpenApiRequestBody requestBody() {
        return getAnnotation("requestBody", OpenApiRequestBodyInstance::new);
    }

    @Override
    public OpenApiResponse[] responses() {
        return getArray("responses", AnnotationMirror.class).stream()
                .map(OpenApiResponseInstance::new)
                .toArray(OpenApiResponse[]::new);
    }

    @Override
    public OpenApiSecurity[] security() {
        return getArray("security", AnnotationMirror.class, OpenApiSecurityInstance::new, OpenApiSecurity[]::new);
    }

    @Override
    public String summary() {
        return getString("summary");
    }

    @Override
    public String[] tags() {
        return getArray("tags", AnnotationValue.class).stream()
                .map(value -> value.getValue().toString())
                .toArray(String[]::new);
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return OpenApi.class;
    }

}
