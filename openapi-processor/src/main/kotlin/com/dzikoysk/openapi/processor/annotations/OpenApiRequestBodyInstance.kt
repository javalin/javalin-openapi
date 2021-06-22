package com.dzikoysk.openapi.processor.annotations

import com.dzikoysk.openapi.annotations.OpenApiRequestBody
import com.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper
import javax.lang.model.element.AnnotationMirror

internal class OpenApiRequestBodyInstance(mirror: AnnotationMirror) : AnnotationMirrorMapper(mirror) {

    fun content(): List<OpenApiContentInstance> =
        getArray("content", AnnotationMirror::class.java) { OpenApiContentInstance(it) }

    fun description(): String =
        getString("description")

    fun required(): Boolean =
        getBoolean("required")

    fun annotationType(): Class<out Annotation> =
        OpenApiRequestBody::class.java

}