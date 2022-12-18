package io.javalin.openapi.processor.shared

import io.javalin.openapi.OpenApiName
import io.javalin.openapi.processor.OpenApiAnnotationProcessor.Companion.context
import javax.lang.model.element.Element
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

fun TypeMirror.isPrimitive(): Boolean =
    kind.isPrimitive

fun TypeMirror.getSimpleName(): String =
    getFullName().substringAfterLast(".")

fun TypeMirror.getFullName(): String =
    context.env.typeUtils.asElement(this)
        ?.getAnnotation(OpenApiName::class.java)
        ?.value
        ?.let { toString().substringBeforeLast(".") + "." + it }
        ?: toString().substringBefore("<")

fun Element.hasAnnotation(simpleName: String): Boolean =
    annotationMirrors.any { it.annotationType.asElement().simpleName.contentEquals(simpleName) }

fun Element.getFullName(): String =
    toString()

fun Element.toSimpleName(): String =
    simpleName.toString()

fun VariableElement.toSimpleName(): String =
    simpleName.toString()