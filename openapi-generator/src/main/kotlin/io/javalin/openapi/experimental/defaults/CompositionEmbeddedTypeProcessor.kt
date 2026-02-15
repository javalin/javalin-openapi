package io.javalin.openapi.experimental.defaults

import io.javalin.openapi.experimental.EmbeddedTypeProcessor
import io.javalin.openapi.experimental.EmbeddedTypeProcessorContext
import io.javalin.openapi.experimental.processor.generators.createComposition

class CompositionEmbeddedTypeProcessor : EmbeddedTypeProcessor {

    override fun process(context: EmbeddedTypeProcessorContext): Boolean =
        context.composition
            ?.let {
                context.scheme.createComposition(
                    context = context.parentContext,
                    classDefinition = context.type,
                    propertyComposition = it,
                    references = context.references,
                    inlineRefs = context.inlineRefs,
                    requiresNonNulls = context.requiresNonNulls
                )
                true
            } ?: false

}