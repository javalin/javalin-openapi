package net.dzikoysk.openapi.processor.annotations

import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.type.TypeMirror

internal class OpenApiPropertyTypeInstance(mirror: AnnotationMirror) : AnnotationMirrorMapper(mirror) {

    fun definedBy(): TypeMirror =
        getType("definedBy")

}