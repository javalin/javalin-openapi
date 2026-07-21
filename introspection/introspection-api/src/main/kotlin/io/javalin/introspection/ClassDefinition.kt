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

    abstract fun getEnumConstants(): List<EnumConstant>

    abstract fun getProperties(): List<PropertyProjection>

    abstract fun getAnnotations(): AnnotationSet

    override fun toString(): String =
        when {
            generics.isEmpty() -> fullName
            else -> "$fullName<${generics.joinToString(", ")}>"
        }
}

enum class StructureType {
    DEFAULT,
    ARRAY,
    DICTIONARY,
}

data class EnumConstant(
    val name: String,
    val annotations: AnnotationSet,
)
