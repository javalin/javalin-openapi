package net.dzikoysk.openapi.processor.annotations

import io.javalin.plugin.openapi.annotations.OpenApiFileUpload
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper
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

    fun annotationType(): Class<out Annotation> =
        OpenApiFileUpload::class.java

}