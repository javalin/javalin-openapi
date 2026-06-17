package io.javalin.introspection

/** A logical property — one per name, merging whatever accessors back it (getter, field, setter). No naming/ignore policy applied. */
class PropertyView(
    val name: String,
    val type: ClassDefinition,
    val accessors: Set<Accessor>,
    val nullable: Boolean,
    val visibility: Visibility,
    val transient: Boolean,
    val annotations: Annotations,
) {
    /** How this property projects: backed by a getter (Java bean / Kotlin property / record) → [Projection.PROPERTY]; field-only → [Projection.FIELD]. */
    val projection: Projection
        get() = if (Accessor.GETTER in accessors) Projection.PROPERTY else Projection.FIELD
}

enum class Accessor { GETTER, FIELD, SETTER }

enum class Projection { PROPERTY, FIELD }

enum class Visibility { PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE }

/** A getter name: `getX` / `isX` with an uppercase char right after the prefix (excludes e.g. `issue`, `getaway`). */
fun isGetterName(name: String): Boolean =
    (name.startsWith("get") && name.length > 3 && name[3].isUpperCase()) ||
        (name.startsWith("is") && name.length > 2 && name[2].isUpperCase())

/** A setter name: `setX` with an uppercase char right after the prefix. */
fun isSetterName(name: String): Boolean =
    name.startsWith("set") && name.length > 3 && name[3].isUpperCase()

/** Logical property name of a getter/setter (`getId`/`setId`→`id`, `isActive`→`active`); call only on names that pass [isGetterName]/[isSetterName]. */
fun propertyName(accessorName: String): String =
    when {
        accessorName.startsWith("get") -> accessorName.removePrefix("get")
        accessorName.startsWith("set") -> accessorName.removePrefix("set")
        else -> accessorName.removePrefix("is")
    }.replaceFirstChar { it.lowercase() }
