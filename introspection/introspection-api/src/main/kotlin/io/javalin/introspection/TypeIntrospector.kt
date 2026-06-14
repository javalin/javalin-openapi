package io.javalin.introspection

/** Resolves a backend-native type token (Class, TypeMirror, KSType) into a [ClassDefinition]. */
fun interface TypeIntrospector {
    fun introspect(source: Any): ClassDefinition
}
