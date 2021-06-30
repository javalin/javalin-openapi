package com.dzikoysk.openapi.processor.annotations

import com.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.type.TypeMirror

internal class OpenApiFormParamInstance internal constructor(mirror: AnnotationMirror) : AnnotationMirrorMapper(mirror) {

    fun name(): String =
        getString("name")

    fun required(): Boolean =
        getBoolean("required")

    fun type(): TypeMirror =
        getType("type")

}