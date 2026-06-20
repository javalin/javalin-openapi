package io.javalin.openapi.experimental.processor.generators

import io.javalin.openapi.Composition
import io.javalin.openapi.experimental.OpenApiType

data class PropertyComposition(
    val type: Composition,
    val references: Set<OpenApiType>,
    val discriminator: DiscriminatorInfo?,
)

data class DiscriminatorInfo(
    val propertyName: String,
    val propertyType: OpenApiType,
    val injectInMappings: Boolean,
    val mapping: List<Pair<String, OpenApiType>>,
)
