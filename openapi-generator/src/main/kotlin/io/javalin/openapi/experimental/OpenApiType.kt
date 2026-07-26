package io.javalin.openapi.experimental

import io.javalin.openapi.experimental.StructureType.DEFAULT

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
    @property:InternalOpenApiTypeApi @JvmField val handle: Any? = null,
) {

    override fun equals(other: Any?): Boolean =
        when {
            this === other -> true
            other is OpenApiType ->
                fullName == other.fullName
                    && generics == other.generics
                    && structureType == other.structureType
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
    DICTIONARY,
}

interface Extra

data class CustomProperty(
    val name: String,
    val type: OpenApiType,
) : Extra

internal fun OpenApiType.mergeExtraFrom(other: OpenApiType): Boolean {
    val missingExtra = other.extra.filterNot(extra::contains)
    if (missingExtra.isEmpty()) return false

    extra.addAll(missingExtra)
    return true
}
