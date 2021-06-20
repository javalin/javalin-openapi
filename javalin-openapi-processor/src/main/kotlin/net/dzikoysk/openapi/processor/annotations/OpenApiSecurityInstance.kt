package net.dzikoysk.openapi.processor.annotations

import io.javalin.plugin.openapi.annotations.OpenApiSecurity
import net.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue

class OpenApiSecurityInstance(mirror: AnnotationMirror) : AnnotationMirrorMapper(mirror) {

    fun name(): String =
        getString("name")

    fun scopes(): List<String> =
        getArray("scopes", AnnotationValue::class.java) { it.value.toString() }

    fun annotationType(): Class<out Annotation> =
        OpenApiSecurity::class.java

}