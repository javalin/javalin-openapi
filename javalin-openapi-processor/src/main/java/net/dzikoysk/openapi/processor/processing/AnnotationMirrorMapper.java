package net.dzikoysk.openapi.processor.processing;

import net.dzikoysk.openapi.processor.processing.ArrayVisitor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

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

    protected <A extends Annotation> A getAnnotation(String key, Function<AnnotationMirror, A> function) {
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

    protected String getString(String key) {
        return getValue(key).toString();
    }

    protected Boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key));
    }

}
