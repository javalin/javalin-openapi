package net.dzikoysk.openapi.processor.processing;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

public final class ExecutableVisitor implements ElementVisitor<ExecutableElement, Void> {

    @Override
    public ExecutableElement visit(Element e, Void unused) {
        throw new UnsupportedOperationException("Not implemented: ExecutableVisitor.visit(Element, Void)");
    }

    @Override
    public ExecutableElement visit(Element e) {
        throw new UnsupportedOperationException("Not implemented: ExecutableVisitor.visit(Element)");
    }

    @Override
    public ExecutableElement visitPackage(PackageElement e, Void unused) {
        throw new UnsupportedOperationException("Not implemented: ExecutableVisitor.visit(Package, Void)");
    }

    @Override
    public ExecutableElement visitType(TypeElement e, Void unused) {
        throw new UnsupportedOperationException("Not implemented: ExecutableVisitor.visit(TypeElement, Void)");
    }

    @Override
    public ExecutableElement visitVariable(VariableElement e, Void unused) {
        throw new UnsupportedOperationException("Not implemented: ExecutableVisitor.visit(VariableElement, Void)");
    }

    @Override
    public ExecutableElement visitExecutable(ExecutableElement e, Void unused) {
        return e;
    }

    @Override
    public ExecutableElement visitTypeParameter(TypeParameterElement e, Void unused) {
        throw new UnsupportedOperationException("Not implemented: ExecutableVisitor.visit(TypeParameterElement, Void)");
    }

    @Override
    public ExecutableElement visitUnknown(Element e, Void unused) {
        throw new UnsupportedOperationException("Not implemented: ExecutableVisitor.visitUnknown(Element, Void)");
    }

}
