package net.dzikoysk.openapi.processor.annotations

import io.javalin.plugin.openapi.annotations.OpenApiParam
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.type.TypeMirror

class OpenApiParamInstance(mirror: AnnotationMirror) : AnnotationMirrorMapper(mirror) {

    enum class In {
        QUERY, HEADER, PATH, COOKIE
    }

    fun allowEmptyValue(): Boolean =
        getBoolean("allowEmptyValue")

    fun deprecated(): Boolean =
        getBoolean("deprecated")

    fun description(): String =
        getString("description")

    fun isRepeatable(): Boolean =
        getBoolean("isRepeatable")

    fun name(): String =
        getString("name")

    fun required(): Boolean =
        getBoolean("required")

    fun type(): TypeMirror =
        getType("type")

    fun annotationType(): Class<out Annotation> =
        OpenApiParam::class.java

}