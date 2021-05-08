package net.dzikoysk.openapi.processor.annotations;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import java.util.Map.Entry;

class AnnotationMirrorMapper {

    protected final AnnotationMirror mirror;

    protected AnnotationMirrorMapper(AnnotationMirror mirror) {
        this.mirror = mirror;
    }

    protected Entry<? extends ExecutableElement, ? extends AnnotationValue> getEntry(String key) {
        return mirror.getElementValues().entrySet().stream()
                .filter(element -> element.getKey().getSimpleName().contentEquals(key))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Missing '" + key + "' property in @OpenApi annotation"));
    }
    protected Object getValue(String key) {
        return getEntry(key).getValue().getValue();
    }

    protected Object[] getArray(String key) {
        return getEntry(key).getValue().accept(new ArrayVisitor<>(), null).toArray();
    }

    protected String getString(String key) {
        return getValue(key).toString();
    }

    protected Boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key));
    }

}
