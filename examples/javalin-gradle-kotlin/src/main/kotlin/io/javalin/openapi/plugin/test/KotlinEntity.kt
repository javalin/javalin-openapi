package io.javalin.openapi.plugin.test

import io.javalin.openapi.JsonSchema
import io.javalin.openapi.plugin.test.JavalinTest.Description

data class KotlinEntity(
    val name: String,
    val value: Int
)

@JsonSchema
@Description(title = "Kotlin Scheme", description = "Example usage of custom annotation on Kotlin class", statusCode = -1)
data class KotlinScheme(
    @get:Description(title = "Value", description = "Int value", statusCode = 200)
    val value: Int
)