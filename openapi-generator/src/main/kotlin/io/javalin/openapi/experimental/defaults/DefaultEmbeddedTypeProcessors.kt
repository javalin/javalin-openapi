package io.javalin.openapi.experimental.defaults

import io.javalin.openapi.experimental.EmbeddedTypeProcessor

/** Default [EmbeddedTypeProcessor] chain (composition → array → dictionary), shared by every backend. */
fun createDefaultEmbeddedTypeProcessors(): MutableList<EmbeddedTypeProcessor> = mutableListOf(
    CompositionEmbeddedTypeProcessor(),
    ArrayEmbeddedTypeProcessor(),
    DictionaryEmbeddedTypeProcessor(),
)
