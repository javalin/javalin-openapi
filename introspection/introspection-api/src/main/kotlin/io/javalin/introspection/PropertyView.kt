package io.javalin.introspection

/** A raw member (getter, field, or record component) — no naming/ignore policy applied. */
class PropertyView(
    val name: String,
    val type: ClassDefinition,
    val accessor: Accessor,
    val nullable: Boolean,
    val annotations: Annotations,
)

enum class Accessor { FIELD, GETTER, RECORD_COMPONENT }
