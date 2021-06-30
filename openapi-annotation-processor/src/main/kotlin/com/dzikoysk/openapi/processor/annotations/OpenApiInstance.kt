package com.dzikoysk.openapi.processor.annotations

import com.dzikoysk.openapi.annotations.HttpMethod
import com.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper
import javax.lang.model.element.AnnotationMirror

internal class OpenApiInstance(mirror: AnnotationMirror) : AnnotationMirrorMapper(mirror) {

    fun composedRequestBody(): OpenApiComposedRequestBodyInstance =
        getAnnotation("composedRequestBody") { OpenApiComposedRequestBodyInstance(it) }

    fun cookies(): List<OpenApiParamInstance> =
        getArray("cookies", AnnotationMirror::class.java) { OpenApiParamInstance(it) }

    fun deprecated(): Boolean =
        getBoolean("deprecated")

    fun description(): String =
        getString("description")

    fun fileUploads(): List<OpenApiFileUploadInstance> =
        getArray("fileUploads", AnnotationMirror::class.java) { OpenApiFileUploadInstance(it) }

    fun formParams(): List<OpenApiFormParamInstance> =
        getArray("formParams", AnnotationMirror::class.java) { OpenApiFormParamInstance(it) }

    fun headers(): List<OpenApiParamInstance> =
        getArray("headers", AnnotationMirror::class.java) { OpenApiParamInstance(it) }

    fun ignore(): Boolean =
        getBoolean("ignore")

    fun methods(): List<HttpMethod> =
        getArray("methods", Any::class.java) { HttpMethod.valueOf(it.toString().toUpperCase()) }

    fun operationId(): String =
        getString("operationId")

    fun path(): String =
        getString("path")

    fun pathParams(): List<OpenApiParamInstance> =
        getArray("pathParams", AnnotationMirror::class.java) { OpenApiParamInstance(it) }

    fun queryParams(): List<OpenApiParamInstance> =
        getArray("queryParams", AnnotationMirror::class.java) { OpenApiParamInstance(it) }

    fun requestBody(): OpenApiRequestBodyInstance =
        getAnnotation("requestBody") { OpenApiRequestBodyInstance(it) }

    fun responses(): List<OpenApiResponseInstance> =
        getArray("responses", AnnotationMirror::class.java).map { OpenApiResponseInstance(it) }

    fun security(): List<OpenApiSecurityInstance> =
        getArray("security", AnnotationMirror::class.java) { OpenApiSecurityInstance(it) }

    fun summary(): String =
        getString("summary")

    fun tags(): List<String> =
        getArray("tags", String::class.java)

}