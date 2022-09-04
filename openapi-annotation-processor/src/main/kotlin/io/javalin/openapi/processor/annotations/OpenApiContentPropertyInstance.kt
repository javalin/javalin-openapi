package io.javalin.openapi.processor.annotations

import io.javalin.openapi.processor.processing.AnnotationMirrorMapper
import javax.lang.model.element.AnnotationMirror

internal class OpenApiContentPropertyInstance(mirror: AnnotationMirror) : AnnotationMirrorMapper(mirror) {

    fun name(): String =
        getString("name")

    fun type(): String =
        getString("type")

    fun format(): String =
        getString("format")

}