package net.dzikoysk.openapi.processor.annotations;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

abstract class DefaultVisitor<R, P> implements AnnotationValueVisitor<R, P> {

    @Override
    public R visit(AnnotationValue av, P p) {
        return null;
    }

    @Override
    public R visit(AnnotationValue av) {
        return null;
    }

    @Override
    public R visitBoolean(boolean b, P p) {
        return null;
    }

    @Override
    public R visitByte(byte b, P p) {
        return null;
    }

    @Override
    public R visitChar(char c, P p) {
        return null;
    }

    @Override
    public R visitDouble(double d, P p) {
        return null;
    }

    @Override
    public R visitFloat(float f, P p) {
        return null;
    }

    @Override
    public R visitInt(int i, P p) {
        return null;
    }

    @Override
    public R visitLong(long i, P p) {
        return null;
    }

    @Override
    public R visitShort(short s, P p) {
        return null;
    }

    @Override
    public R visitString(String s, P p) {
        return null;
    }

    @Override
    public R visitType(TypeMirror t, P p) {
        return null;
    }

    @Override
    public R visitEnumConstant(VariableElement c, P p) {
        return null;
    }

    @Override
    public R visitAnnotation(AnnotationMirror a, P p) {
        return null;
    }

    @Override
    public R visitArray(List<? extends AnnotationValue> vals, P p) {
        return null;
    }

    @Override
    public R visitUnknown(AnnotationValue av, P p) {
        return null;
    }

}
