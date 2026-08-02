package io.javalin.openapi.experimental.defaults

import io.javalin.openapi.experimental.EmbeddedTypeProcessor
import io.javalin.openapi.experimental.EmbeddedTypeProcessorContext
import io.javalin.openapi.experimental.processor.generators.createComposition

class CompositionEmbeddedTypeProcessor : EmbeddedTypeProcessor {

    override fun process(context: EmbeddedTypeProcessorContext): Boolean {
        val composition = context.composition ?: return false

        context.scheme.createComposition(
            context = context.parentContext,
            type = context.type,
            propertyComposition = composition,
            references = context.references,
            inlineRefs = context.inlineRefs,
            requiresNonNulls = context.requiresNonNulls,
        )
        return true
    }
}
