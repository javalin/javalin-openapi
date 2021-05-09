package net.dzikoysk.openapi.processor.processing;

import javax.lang.model.type.TypeMirror;

public final class ClassVisitor extends DefaultVisitor<TypeMirror, Void> {

    @Override
    public TypeMirror visitType(TypeMirror typeMirror, Void unused) {
        return typeMirror;
    }

}
