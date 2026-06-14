package io.javalin.introspection.test

import io.javalin.introspection.Accessor
import io.javalin.introspection.ClassDefinition
import io.javalin.introspection.StructureType
import io.javalin.introspection.TypeIntrospector

/** A backend-neutral, order-independent snapshot of one type's introspection, for comparing backends. */
data class TypeSnapshot(
    val fullName: String,
    val simpleName: String,
    val structure: StructureType,
    val isEnum: Boolean,
    val enumConstants: List<String>?,
    val properties: List<PropertySnapshot>,
)

data class PropertySnapshot(
    val name: String,
    val typeFullName: String,
    val typeStructure: StructureType,
    val typeGenerics: List<String>,
    val accessor: Accessor,
    val nullable: Boolean,
)

fun snapshot(introspector: TypeIntrospector, type: ClassDefinition): TypeSnapshot {
    val enum = introspector.isEnum(type)
    return TypeSnapshot(
        fullName = type.fullName,
        simpleName = type.simpleName,
        structure = type.structureType,
        isEnum = enum,
        enumConstants = introspector.enumConstants(type)?.sorted(),
        properties = if (enum) emptyList() else introspector.properties(type)
            .map { PropertySnapshot(it.name, it.type.fullName, it.type.structureType, it.type.generics.map { g -> g.fullName }, it.accessor, it.nullable) }
            .sortedBy { "${it.accessor}:${it.name}" },
    )
}
