package io.javalin.openapi.experimental.processor.shared

import io.javalin.openapi.experimental.AnnotationProcessorContext
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

fun AnnotationProcessorContext.objectType(): TypeElement = forTypeElement(Object::class.java.name)!!
fun AnnotationProcessorContext.mapType(): TypeElement = forTypeElement(Map::class.java.name)!!

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
