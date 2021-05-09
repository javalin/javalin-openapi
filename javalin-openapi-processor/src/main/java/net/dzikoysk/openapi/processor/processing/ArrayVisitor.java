package net.dzikoysk.openapi.processor.processing;

import javax.lang.model.element.AnnotationValue;
import java.util.List;
import java.util.stream.Collectors;

public final class ArrayVisitor<T> extends DefaultVisitor<List<? extends T>, Void> {

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends T> visitArray(List<? extends AnnotationValue> values, Void unused) {
        return values.stream()
                .map(value -> (T) value.getValue())
                .collect(Collectors.toList());
    }

}
