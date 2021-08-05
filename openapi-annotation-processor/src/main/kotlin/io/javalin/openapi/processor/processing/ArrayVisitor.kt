package io.javalin.openapi.processor.processing

import javax.lang.model.element.AnnotationValue

internal class ArrayVisitor<T> : DefaultVisitor<List<T>, Void?>() {

    @Suppress("UNCHECKED_CAST", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun visitArray(values: List<AnnotationValue>, unused: Void?): List<T> =
        values.map { value: AnnotationValue -> value.value as T }

}