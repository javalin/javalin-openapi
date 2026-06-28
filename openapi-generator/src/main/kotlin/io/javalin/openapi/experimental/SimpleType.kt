package io.javalin.openapi.experimental

data class SimpleType @JvmOverloads constructor(
    val type: String,
    val format: String? = null
)
