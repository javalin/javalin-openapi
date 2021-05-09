package net.dzikoysk.openapi.processor.processing;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public abstract class DefaultVisitor<R, P> implements AnnotationValueVisitor<R, P> {

    @Override
    public R visit(AnnotationValue av, P p) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public R visit(AnnotationValue av) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public R visitBoolean(boolean b, P p) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public R visitByte(byte b, P p) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public R visitChar(char c, P p) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public R visitDouble(double d, P p) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public R visitFloat(float f, P p) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public R visitInt(int i, P p) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public R visitLong(long i, P p) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public R visitShort(short s, P p) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public R visitString(String s, P p) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public R visitType(TypeMirror t, P p) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public R visitEnumConstant(VariableElement c, P p) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public R visitAnnotation(AnnotationMirror a, P p) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public R visitArray(List<? extends AnnotationValue> vals, P p) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public R visitUnknown(AnnotationValue av, P p) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
