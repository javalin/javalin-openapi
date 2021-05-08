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

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
public final class OpenApiInstance extends AnnotationMirrorMapper implements OpenApi {

    public OpenApiInstance(AnnotationMirror mirror) {
        super(mirror);
    }

    @Override
    public OpenApiComposedRequestBody composedRequestBody() {
        return null;
    }

    @Override
    public OpenApiParam[] cookies() {
        return new OpenApiParam[0];
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
        return new OpenApiFileUpload[0];
    }

    @Override
    public OpenApiFormParam[] formParams() {
        return new OpenApiFormParam[0];
    }

    @Override
    public OpenApiParam[] headers() {
        return new OpenApiParam[0];
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
        return new OpenApiParam[0];
    }

    @Override
    public OpenApiParam[] queryParams() {
        return new OpenApiParam[0];
    }

    @Override
    public OpenApiRequestBody requestBody() {
        return null;
    }

    @Override
    public OpenApiResponse[] responses() {
        return new OpenApiResponse[0];
    }

    @Override
    public OpenApiSecurity[] security() {
        return new OpenApiSecurity[0];
    }

    @Override
    public String summary() {
        return getString("summary");
    }

    @Override
    public String[] tags() {
        return new String[0];
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }

}
