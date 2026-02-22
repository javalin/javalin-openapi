package io.javalin.openapi.experimental.processor.generators

import io.javalin.openapi.Composition
import io.javalin.openapi.Discriminator
import io.javalin.openapi.experimental.ClassDefinition

data class PropertyComposition(
    val type: Composition,
    val references: Set<ClassDefinition>,
    val discriminator: Discriminator
)
