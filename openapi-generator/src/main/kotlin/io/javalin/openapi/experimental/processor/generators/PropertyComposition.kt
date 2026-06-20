package io.javalin.openapi.experimental.processor.generators

import io.javalin.openapi.Composition
import io.javalin.openapi.experimental.ClassDefinition

data class PropertyComposition(
    val type: Composition,
    val references: Set<ClassDefinition>,
    val discriminator: DiscriminatorInfo?,
)

data class DiscriminatorInfo(
    val propertyName: String,
    val propertyType: ClassDefinition,
    val injectInMappings: Boolean,
    val mapping: List<Pair<String, ClassDefinition>>,
)
