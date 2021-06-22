package net.dzikoysk.openapi.processor.annotations

import net.dzikoysk.openapi.annotations.OpenApiFormParam
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.type.TypeMirror

internal class OpenApiFormParamInstance internal constructor(mirror: AnnotationMirror) : AnnotationMirrorMapper(mirror) {

    fun name(): String =
        getString("name")

    fun required(): Boolean =
        getBoolean("required")

    fun type(): TypeMirror =
        getType("type")

    fun annotationType(): Class<out Annotation> =
        OpenApiFormParam::class.java

}