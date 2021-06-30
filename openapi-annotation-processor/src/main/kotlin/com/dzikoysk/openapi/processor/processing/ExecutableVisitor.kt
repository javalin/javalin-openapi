package com.dzikoysk.openapi.processor.processing

import javax.lang.model.element.Element
import javax.lang.model.element.ElementVisitor
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.element.VariableElement

internal class ExecutableVisitor : ElementVisitor<ExecutableElement, Void?> {

    override fun visit(e: Element, unused: Void?): ExecutableElement {
        throw UnsupportedOperationException("Not implemented: ExecutableVisitor.visit(Element, Void)")
    }

    override fun visit(e: Element): ExecutableElement {
        throw UnsupportedOperationException("Not implemented: ExecutableVisitor.visit(Element)")
    }

    override fun visitPackage(e: PackageElement, unused: Void?): ExecutableElement {
        throw UnsupportedOperationException("Not implemented: ExecutableVisitor.visit(Package, Void)")
    }

    override fun visitType(e: TypeElement, unused: Void?): ExecutableElement {
        throw UnsupportedOperationException("Not implemented: ExecutableVisitor.visit(TypeElement, Void)")
    }

    override fun visitVariable(e: VariableElement, unused: Void?): ExecutableElement {
        throw UnsupportedOperationException("Not implemented: ExecutableVisitor.visit(VariableElement, Void)")
    }

    override fun visitExecutable(e: ExecutableElement, unused: Void?): ExecutableElement {
        return e
    }

    override fun visitTypeParameter(e: TypeParameterElement, unused: Void?): ExecutableElement {
        throw UnsupportedOperationException("Not implemented: ExecutableVisitor.visit(TypeParameterElement, Void)")
    }

    override fun visitUnknown(e: Element, unused: Void?): ExecutableElement {
        throw UnsupportedOperationException("Not implemented: ExecutableVisitor.visitUnknown(Element, Void)")
    }

}