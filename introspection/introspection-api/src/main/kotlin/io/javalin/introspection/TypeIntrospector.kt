package io.javalin.introspection

interface TypeIntrospector {
    fun introspect(source: Any): ClassDefinition
    fun isEnum(type: ClassDefinition): Boolean
    fun enumConstants(type: ClassDefinition): List<String>?
    fun properties(type: ClassDefinition): List<PropertyView>
    fun annotations(type: ClassDefinition): Annotations
}
