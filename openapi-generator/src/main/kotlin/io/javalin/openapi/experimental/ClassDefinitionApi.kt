package io.javalin.openapi.experimental

import io.javalin.openapi.experimental.StructureType.DEFAULT

class ClassDefinition(
    val simpleName: String,
    val fullName: String,
    val generics: List<ClassDefinition> = emptyList(),
    val structureType: StructureType = DEFAULT,
    val extra: MutableList<Extra> = mutableListOf(),
    @JvmField val handle: Any? = null
) {

    override fun equals(other: Any?): Boolean =
        when {
            this === other -> true
            other is ClassDefinition ->
                this.fullName == other.fullName
                    && this.generics == other.generics
                    && this.structureType == other.structureType
            else -> false
        }

    override fun hashCode(): Int {
        var result = fullName.hashCode()
        result = 31 * result + generics.hashCode()
        result = 31 * result + structureType.hashCode()
        return result
    }

    override fun toString(): String =
        when {
            generics.isEmpty() -> fullName
            else -> "$fullName<${generics.joinToString(", ")}>"
        }

}

enum class StructureType {
    DEFAULT,
    ARRAY,
    DICTIONARY
}

interface Extra

class CustomProperty(
    val name: String,
    val type: ClassDefinition
) : Extra
