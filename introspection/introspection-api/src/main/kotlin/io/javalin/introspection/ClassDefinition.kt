package io.javalin.introspection

/** [handle] carries the backend's native token (Class / TypeMirror / KSType). */
class ClassDefinition(
    val simpleName: String,
    val fullName: String,
    val generics: List<ClassDefinition> = emptyList(),
    val structureType: StructureType = StructureType.DEFAULT,
    @JvmField val handle: Any? = null,
) {

    override fun equals(other: Any?): Boolean =
        this === other || (other is ClassDefinition &&
            fullName == other.fullName && generics == other.generics && structureType == other.structureType)

    override fun hashCode(): Int {
        var result = fullName.hashCode()
        result = 31 * result + generics.hashCode()
        result = 31 * result + structureType.hashCode()
        return result
    }

    override fun toString(): String =
        if (generics.isEmpty()) fullName else "$fullName<${generics.joinToString(", ")}>"
}

enum class StructureType { DEFAULT, ARRAY, DICTIONARY }
