package net.dzikoysk.openapi.processor.processing;

import javax.lang.model.element.AnnotationMirror;

final class AnnotationVisitor extends DefaultVisitor<AnnotationMirror, Void> {

    @Override
    public AnnotationMirror visitAnnotation(AnnotationMirror a, Void unused) {
        return a;
    }

}
