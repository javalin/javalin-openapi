package io.javalin.openapi.dynamic

import io.javalin.openapi.experimental.ClassDefinition
import java.lang.reflect.Type

/** Reflection counterpart of the AP's `ClassDefinitionHandle`, stored in [ClassDefinition.handle]. */
data class ReflectionHandle(
    val erasure: Class<*>,
    val type: Type,
)

val ClassDefinition.reflectedClass: Class<*>
    get() = (handle as ReflectionHandle).erasure

val ClassDefinition.reflectedType: Type
    get() = (handle as ReflectionHandle).type
