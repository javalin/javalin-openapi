package io.javalin.openapi.experimental

import io.javalin.openapi.experimental.StructureType.DEFAULT
import java.util.Objects

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
    @property:InternalOpenApiTypeApi val handle: Any? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is OpenApiType
            && fullName == other.fullName
            && generics == other.generics
            && structureType == other.structureType

    override fun hashCode(): Int = Objects.hash(fullName, generics, structureType)
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
