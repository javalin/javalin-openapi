package io.javalin.openapi.processor.shared

import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

fun TypeMirror.isPrimitive(): Boolean =
    kind.isPrimitive

fun Element.hasAnnotation(simpleName: String): Boolean =
    annotationMirrors.any { it.annotationType.asElement().simpleName.contentEquals(simpleName) }