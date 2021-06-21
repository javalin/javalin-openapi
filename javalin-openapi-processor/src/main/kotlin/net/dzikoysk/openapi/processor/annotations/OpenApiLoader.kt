package net.dzikoysk.openapi.processor.annotations

import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

internal object OpenApiLoader {

    fun loadAnnotations(annotations: Set<TypeElement>, elements: Elements, types: Types, roundEnv: RoundEnvironment): Collection<OpenApiInstance> {
        val openApiAnnotations: MutableCollection<OpenApiInstance> = ArrayList()

        for (annotation in annotations) {
            val annotatedElements = roundEnv.getElementsAnnotatedWith(annotation)

            for (annotatedElement in annotatedElements) {
                val openApiAnnotationMirror = annotatedElement.annotationMirrors[0]
                val openApiInstance = OpenApiInstance(openApiAnnotationMirror)
                openApiAnnotations.add(openApiInstance)
            }
        }

        return openApiAnnotations
    }

}