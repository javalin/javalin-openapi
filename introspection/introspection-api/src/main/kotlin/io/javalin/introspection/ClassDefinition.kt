package io.javalin.introspection

abstract class ClassDefinition(
    val simpleName: String,
    val fullName: String,
    val generics: List<ClassDefinition> = emptyList(),
    val structureType: StructureType = StructureType.DEFAULT,
) {

    @InternalIntrospectionApi
    abstract val source: Any

    abstract fun isEnum(): Boolean

    abstract fun getEnumConstants(): List<EnumConstantView>?

    abstract fun getProperties(): List<PropertyView>

    abstract fun getAnnotations(): Annotations

    override fun toString(): String =
        if (generics.isEmpty()) fullName else "$fullName<${generics.joinToString(", ")}>"
}

enum class StructureType { DEFAULT, ARRAY, DICTIONARY }

class EnumConstantView(val name: String, val annotations: Annotations)
