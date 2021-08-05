package io.javalin.openapi.processor.annotations

import io.javalin.openapi.processor.processing.AnnotationMirrorMapper
import javax.lang.model.element.AnnotationMirror

internal class OpenApiComposedRequestBodyInstance(mirror: AnnotationMirror) : AnnotationMirrorMapper(mirror) {

    fun anyOf(): List<OpenApiContentInstance> =
        getArray("anyOf", AnnotationMirror::class.java) { OpenApiContentInstance(it) }

    fun contentType(): String =
        getString("contentType")

    fun description(): String =
        getString("description")

    fun oneOf(): List<OpenApiContentInstance> =
        getArray("oneOf", AnnotationMirror::class.java) { OpenApiContentInstance(it) }

    fun required(): Boolean =
        getBoolean("required")

}