package net.dzikoysk.openapi.processor.processing;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;

final class AnnotationVisitor<A extends Annotation> extends DefaultVisitor<A, Void> {

    @Override
    public A visitAnnotation(AnnotationMirror a, Void unused) {
        return super.visitAnnotation(a, unused);
    }

}
