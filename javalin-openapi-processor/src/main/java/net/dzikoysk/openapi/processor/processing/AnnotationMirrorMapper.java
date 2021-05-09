package net.dzikoysk.openapi.processor.processing;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.IntFunction;

public class AnnotationMirrorMapper {

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

    protected <R> R getAnnotation(String key, Function<AnnotationMirror, R> function) {
        return function.apply(getEntry(key).getValue().accept(new AnnotationVisitor<>(), null));
    }

    protected <T, R> R[] getArray(String key, Class<T> type, Function<T, R> mapper, IntFunction<R[]> arraySupplier) {
        return getArray(key, type).stream()
                .map(mapper)
                .toArray(arraySupplier);
    }

    protected <T> List<? extends T> getArray(String key, Class<T> type) {
        return getEntry(key).getValue().accept(new ArrayVisitor<T>(), null);
    }

    protected TypeMirror getType(String key) {
        return getEntry(key).getValue().accept(new ClassVisitor(), null);
    }

    protected String getString(String key) {
        return getValue(key).toString();
    }

    protected Boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key));
    }

}
