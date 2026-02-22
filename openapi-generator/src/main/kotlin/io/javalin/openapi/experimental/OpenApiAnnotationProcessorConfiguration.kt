package io.javalin.openapi.experimental

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

@RequiresOptIn
@Retention(BINARY)
@Target(CLASS, FUNCTION)
annotation class ExperimentalCompileOpenApiConfiguration

data class SimpleType @JvmOverloads constructor(
    val type: String,
    val format: String? = null
)
