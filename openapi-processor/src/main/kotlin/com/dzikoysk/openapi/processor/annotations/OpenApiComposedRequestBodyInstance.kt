package com.dzikoysk.openapi.processor.annotations

import com.dzikoysk.openapi.annotations.OpenApiComposedRequestBody
import com.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper
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

    fun annotationType(): Class<out Annotation> =
        OpenApiComposedRequestBody::class.java

}