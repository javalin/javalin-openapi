package io.javalin.introspection

fun interface TypeIntrospector {
    fun introspect(source: Any): ClassDefinition
}
