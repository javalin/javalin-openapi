package net.dzikoysk.openapi.processor.processing

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.AnnotationValueVisitor
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

abstract class DefaultVisitor<R, P> : AnnotationValueVisitor<R, P> {

    override fun visit(av: AnnotationValue, p: P): R {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun visit(av: AnnotationValue): R {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun visitBoolean(b: Boolean, p: P): R {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun visitByte(b: Byte, p: P): R {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun visitChar(c: Char, p: P): R {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun visitDouble(d: Double, p: P): R {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun visitFloat(f: Float, p: P): R {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun visitInt(i: Int, p: P): R {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun visitLong(i: Long, p: P): R {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun visitShort(s: Short, p: P): R {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun visitString(s: String, p: P): R {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun visitType(t: TypeMirror, p: P): R {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun visitEnumConstant(c: VariableElement, p: P): R {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun visitAnnotation(a: AnnotationMirror, p: P): R {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun visitArray(vals: List<AnnotationValue>, p: P): R {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun visitUnknown(av: AnnotationValue, p: P): R {
        throw UnsupportedOperationException("Not implemented")
    }

}