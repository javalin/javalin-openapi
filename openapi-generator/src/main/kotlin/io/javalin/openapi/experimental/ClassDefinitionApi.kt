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
            other is ClassDefinition -> this.fullName == other.fullName
            else -> false
        }

    override fun hashCode(): Int = fullName.hashCode()

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
