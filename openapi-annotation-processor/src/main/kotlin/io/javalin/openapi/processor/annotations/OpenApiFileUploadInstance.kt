package io.javalin.openapi.processor.annotations

import io.javalin.openapi.processor.processing.AnnotationMirrorMapper
import javax.lang.model.element.AnnotationMirror

internal class OpenApiFileUploadInstance(mirror: AnnotationMirror) : AnnotationMirrorMapper(mirror) {

    fun description(): String =
        getString("description")

    fun isArray(): Boolean =
        getBoolean("isArray")

    fun name(): String =
        getString("name")

    fun required(): Boolean =
        getBoolean("required")

}