package io.javalin.introspection

/** A type resolved by a backend; navigation is implemented per backend. */
abstract class ClassDefinition(
    val simpleName: String,
    val fullName: String,
    val generics: List<ClassDefinition> = emptyList(),
    val structureType: StructureType = StructureType.DEFAULT,
) {

    /** Backend-native token this was resolved from (`Class`, `TypeMirror`, ...); only the producing backend may cast it. */
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

/** An enum constant paired with its declared annotations (e.g. for `@OpenApiName` overrides). */
class EnumConstantView(val name: String, val annotations: Annotations)
