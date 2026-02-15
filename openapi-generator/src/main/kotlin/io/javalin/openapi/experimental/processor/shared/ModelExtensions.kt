package io.javalin.openapi.experimental.processor.shared

import io.javalin.openapi.experimental.AnnotationProcessorContext
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

fun AnnotationProcessorContext.objectType(): TypeElement = forTypeElement(Object::class.java.name)!!
fun AnnotationProcessorContext.collectionType(): TypeElement = forTypeElement(Collection::class.java.name)!!
fun AnnotationProcessorContext.mapType(): TypeElement = forTypeElement(Map::class.java.name)!!
fun AnnotationProcessorContext.recordType(): TypeElement? = forTypeElement("java.lang.Record")

fun TypeMirror.isPrimitive(): Boolean =
    kind.isPrimitive

fun Element.hasAnnotation(simpleName: String): Boolean =
    annotationMirrors.any { it.annotationType.asElement().simpleName.contentEquals(simpleName) }

fun Element.getFullName(): String =
    toString()

fun Element.toSimpleName(): String =
    simpleName.toString()

fun VariableElement.toSimpleName(): String =
    simpleName.toString()

fun <A : Annotation> A.getTypeMirrors(supplier: A.() -> Array<out KClass<*>>): Set<TypeMirror> =
    try {
        throw Error(supplier().toString()) // always throws MirroredTypesException, because we cannot get Class instance from annotation at compile-time
    } catch (mirroredTypeException: MirroredTypesException) {
        mirroredTypeException.typeMirrors.toSet()
    }

fun <A : Annotation, K : KClass<*>> A.getTypeMirror(supplier: A.() -> K): TypeMirror =
    try {
        throw Error(supplier().toString()) // always throws MirroredTypeException, because we cannot get Class instance from annotation at compile-time
    } catch (mirroredTypeException: MirroredTypeException) {
        mirroredTypeException.typeMirror
    }