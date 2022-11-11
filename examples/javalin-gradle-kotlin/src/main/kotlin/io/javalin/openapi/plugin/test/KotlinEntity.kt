package io.javalin.openapi.plugin.test

import io.javalin.openapi.CustomAnnotation
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.OneOf
import io.javalin.openapi.plugin.test.JavalinTest.Description

@JsonSchema(
    generateResource = false,
    requireNonNulls = false
)
data class KotlinEntity(
    val name: String,
    val value: Int,
    val elements: Elements
)

@JsonSchema(
    requireNonNulls = false
)
@Description(
    title = "Kotlin Scheme",
    description =
        """
        Example usage of custom annotation on Kotlin class
        """,
    statusCode = -1
)
data class KotlinScheme(
    @get:Description(title = "Value", description = "Int value", statusCode = 200)
    val value: Int,
    @get:OneOf(KotlinEntity::class)
    val any: Any
)

@CustomAnnotation
@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class CustomAnnotationInKotlinWithArray(
    // should support arrays
    val value: Array<String> = [],
)

@JsonSchema
data class Elements(
    @get:CustomAnnotationInKotlinWithArray(["a", "b"])
    val value: String
)