package net.dzikoysk.openapi.processor.annotations

import io.javalin.plugin.openapi.annotations.OpenApiContent
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.type.TypeMirror

class OpenApiContentInstance(mirror: AnnotationMirror) : AnnotationMirrorMapper(mirror) {

    fun from(): TypeMirror =
        getType("from")

    fun isArray(): Boolean =
        getBoolean("isArray")

    fun type(): String =
        getString("type")

    fun annotationType(): Class<out Annotation> =
        OpenApiContent::class.java

}