package io.javalin.openapi.processor.shared

import javax.lang.model.element.Element
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

fun TypeMirror.isPrimitive(): Boolean =
    kind.isPrimitive

fun TypeMirror.getFullName(): String =
    toString()

fun Element.hasAnnotation(simpleName: String): Boolean =
    annotationMirrors.any { it.annotationType.asElement().simpleName.contentEquals(simpleName) }

fun Element.getFullName(): String =
    toString()

fun Element.toSimpleName(): String =
    simpleName.toString()

fun VariableElement.toSimpleName(): String =
    simpleName.toString()