package net.dzikoysk.openapi.processor.annotations

import net.dzikoysk.openapi.annotations.OpenApiSecurity
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper
import javax.lang.model.element.AnnotationMirror

internal class OpenApiSecurityInstance(mirror: AnnotationMirror) : AnnotationMirrorMapper(mirror) {

    fun name(): String =
        getString("name")

    fun scopes(): List<String> =
        getArray("scopes", String::class.java)

    fun annotationType(): Class<out Annotation> =
        OpenApiSecurity::class.java

}