package io.javalin.openapi.processor.annotations

import io.javalin.openapi.Visibility
import io.javalin.openapi.processor.processing.AnnotationMirrorMapper
import javax.lang.model.element.AnnotationMirror

internal class OpenApiByFieldsInstance(mirror: AnnotationMirror) : AnnotationMirrorMapper(mirror) {

    fun value(): Visibility =
        Visibility.valueOf(getValue("value").toString())

}