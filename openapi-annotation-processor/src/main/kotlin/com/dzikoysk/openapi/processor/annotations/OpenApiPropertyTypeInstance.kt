package com.dzikoysk.openapi.processor.annotations

import com.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.type.TypeMirror

internal class OpenApiPropertyTypeInstance(mirror: AnnotationMirror) : AnnotationMirrorMapper(mirror) {

    fun definedBy(): TypeMirror =
        getType("definedBy")

}