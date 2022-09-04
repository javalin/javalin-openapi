package io.javalin.openapi.processor.annotations

import io.javalin.openapi.processor.processing.AnnotationMirrorMapper
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.type.TypeMirror

internal class OpenApiContentInstance(mirror: AnnotationMirror) : AnnotationMirrorMapper(mirror) {

    fun from(): TypeMirror =
        getType("from")

    fun mimeType(): String =
        getString("mimeType")

    fun type(): String =
        getString("type")

    fun format(): String =
        getString("format")

    fun properties(): List<OpenApiContentPropertyInstance> =
        getArray("properties", AnnotationMirror::class.java) { OpenApiContentPropertyInstance(it) }

}