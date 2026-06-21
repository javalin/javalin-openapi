package io.javalin.openapi.experimental.processor.shared

import io.javalin.openapi.experimental.AnnotationProcessorContext
import javax.lang.model.element.TypeElement

fun AnnotationProcessorContext.objectType(): TypeElement = forTypeElement(Object::class.java.name)!!
fun AnnotationProcessorContext.mapType(): TypeElement = forTypeElement(Map::class.java.name)!!
