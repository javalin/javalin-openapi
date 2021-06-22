package net.dzikoysk.openapi.annotations

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.reflect.KClass

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
@Retention(SOURCE)
annotation class OpenApiPropertyType(val definedBy: KClass<*>)