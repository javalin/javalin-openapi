package io.javalin.openapi.experimental

import io.javalin.openapi.experimental.StructureType.DEFAULT

/**
 * Opt-in marker for [OpenApiType.handle] — the backend-native token (`OpenApiTypeHandle`, a neutral
 * `io.javalin.introspection.ClassDefinition`, ...). Only the backend that produced an [OpenApiType] may cast it;
 * backend-agnostic generator code must navigate via [SchemaGenerationContext] instead.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "handle is the backend-native token behind OpenApiType; only the producing backend may cast it.",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class InternalOpenApiTypeApi

class OpenApiType(
    val simpleName: String,
    val fullName: String,
    val generics: List<OpenApiType> = emptyList(),
    val structureType: StructureType = DEFAULT,
    val extra: MutableList<Extra> = mutableListOf(),
    @property:InternalOpenApiTypeApi @JvmField val handle: Any? = null
) {

    override fun equals(other: Any?): Boolean =
        when {
            this === other -> true
            other is OpenApiType ->
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
    val type: OpenApiType
) : Extra
