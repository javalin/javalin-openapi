package io.javalin.introspection

/** A raw member (getter, field, or record component) — no naming/ignore policy applied. */
class PropertyView(
    val name: String,
    val type: ClassDefinition,
    val accessor: Accessor,
    val nullable: Boolean,
    val visibility: Visibility,
    val transient: Boolean,
    val annotations: Annotations,
)

enum class Accessor { FIELD, GETTER, RECORD_COMPONENT }

enum class Visibility { PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE }

/** JavaBean getter name: `getX` / `isX` with an uppercase char right after the prefix (excludes e.g. `issue`, `getaway`). */
fun isGetterName(name: String): Boolean =
    (name.startsWith("get") && name.length > 3 && name[3].isUpperCase()) ||
        (name.startsWith("is") && name.length > 2 && name[2].isUpperCase())
