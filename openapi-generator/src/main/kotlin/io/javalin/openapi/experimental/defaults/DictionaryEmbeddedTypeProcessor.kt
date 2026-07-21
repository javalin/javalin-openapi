package io.javalin.openapi.experimental.defaults

import com.fasterxml.jackson.databind.JsonNode
import io.javalin.openapi.experimental.EmbeddedTypeProcessor
import io.javalin.openapi.experimental.EmbeddedTypeProcessorContext
import io.javalin.openapi.experimental.StructureType.DICTIONARY
import io.javalin.openapi.experimental.processor.shared.createObjectNode

class DictionaryEmbeddedTypeProcessor : EmbeddedTypeProcessor {

    override fun process(context: EmbeddedTypeProcessorContext): Boolean {
        if (context.type.structureType != DICTIONARY) {
            return false
        }

        context.scheme.put("type", "object")
        val additionalProperties = createObjectNode()
        val additionalType = context.type.generics[1]
        val additionalContext = context.copy(
            scheme = additionalProperties,
            type = additionalType,
        )
        val handled = context.parentContext.embeddedTypeProcessors.any {
            it.process(additionalContext)
        }

        if (!handled) {
            context.parentContext.typeSchemaGenerator.addType(
                scheme = additionalProperties,
                type = additionalType,
                inlineRefs = context.inlineRefs,
                references = context.references,
                requiresNonNulls = context.requiresNonNulls,
            )
        }

        context.scheme.set<JsonNode>("additionalProperties", additionalProperties)
        return true
    }
}
