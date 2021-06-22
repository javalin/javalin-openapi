package com.dzikoysk.openapi.processor.processing

import javax.lang.model.element.AnnotationMirror

internal class AnnotationVisitor : DefaultVisitor<AnnotationMirror, Void?>() {

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun visitAnnotation(annotationMirror: AnnotationMirror, unused: Void?): AnnotationMirror = annotationMirror

}