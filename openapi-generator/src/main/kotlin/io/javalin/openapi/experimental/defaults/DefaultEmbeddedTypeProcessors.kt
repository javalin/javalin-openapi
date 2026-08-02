package io.javalin.openapi.experimental.defaults

import io.javalin.openapi.experimental.EmbeddedTypeProcessor

fun createDefaultEmbeddedTypeProcessors(): MutableList<EmbeddedTypeProcessor> = mutableListOf(
    CompositionEmbeddedTypeProcessor(),
    ArrayEmbeddedTypeProcessor(),
    DictionaryEmbeddedTypeProcessor(),
)
