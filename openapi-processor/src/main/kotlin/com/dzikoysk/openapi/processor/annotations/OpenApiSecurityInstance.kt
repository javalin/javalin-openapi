package com.dzikoysk.openapi.processor.annotations

import com.dzikoysk.openapi.processor.processing.AnnotationMirrorMapper
import javax.lang.model.element.AnnotationMirror

internal class OpenApiSecurityInstance(mirror: AnnotationMirror) : AnnotationMirrorMapper(mirror) {

    fun name(): String =
        getString("name")

    fun scopes(): List<String> =
        getArray("scopes", String::class.java)

}