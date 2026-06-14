package io.javalin.openapi.dynamic

import io.javalin.openapi.experimental.ClassDefinition

/** Reflection counterpart of the AP's `ClassDefinitionHandle`, stored in [ClassDefinition.handle]. */
data class ReflectionHandle(val erasure: Class<*>)

val ClassDefinition.reflectedClass: Class<*>
    get() = (handle as ReflectionHandle).erasure
