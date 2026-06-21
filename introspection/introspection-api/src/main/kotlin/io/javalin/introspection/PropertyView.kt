package io.javalin.introspection

class PropertyView(
    val name: String,
    val type: ClassDefinition,
    val accessor: Accessor,
    val nullable: Boolean,
    val visibility: Visibility,
    val transient: Boolean,
    @property:InternalIntrospectionApi val source: Any,
    val annotations: Annotations,
)

enum class Accessor { FIELD, GETTER, RECORD_COMPONENT }

enum class Visibility { PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE }

fun isGetterName(name: String): Boolean =
    (name.startsWith("get") && name.length > 3 && name[3].isUpperCase()) ||
        (name.startsWith("is") && name.length > 2 && name[2].isUpperCase())

fun propertyName(getterName: String): String =
    (if (getterName.startsWith("get")) getterName.removePrefix("get") else getterName.removePrefix("is"))
        .replaceFirstChar { it.lowercase() }
