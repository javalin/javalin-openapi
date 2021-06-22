package com.dzikoysk.openapi.processor.annotations

import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

internal object OpenApiLoader {

    fun loadAnnotations(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Collection<OpenApiInstance> {
        val openApiAnnotations: MutableCollection<OpenApiInstance> = ArrayList()

        for (annotation in annotations) {
            val annotatedElements = roundEnv.getElementsAnnotatedWith(annotation)

            for (annotatedElement in annotatedElements) {
                annotatedElement.annotationMirrors
                    .filter { mirror -> mirror.annotationType.asElement().simpleName.contentEquals(annotation.simpleName) }
                    .forEach { openApiAnnotations.add(OpenApiInstance(it)) }
            }
        }

        return openApiAnnotations
    }

}