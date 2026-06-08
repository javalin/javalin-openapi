package io.javalin.openapi.experimental

import io.javalin.openapi.experimental.processor.generators.Property

/**
 * Backend-agnostic contract for turning a native type token (`TypeMirror`, `KSType`, [java.lang.reflect.Type], ...) into the shared [ClassDefinition] / [Property] model,
 * so schema generation stays decoupled from any single type system (kapt, KSP, runtime reflection).
 */
interface TypeIntrospector {

    /** Resolve a backend-native type token into a [ClassDefinition] (array/map/generic resolution included). */
    fun introspect(nativeType: Any): ClassDefinition

    fun isEnum(type: ClassDefinition): Boolean

    /** Enum constant names, or `null` if [type] is not an enum. */
    fun enumConstants(type: ClassDefinition): List<String>?

    fun properties(type: ClassDefinition): List<Property>

    fun <A : Annotation> annotation(type: ClassDefinition, annotationType: Class<A>): A?
}