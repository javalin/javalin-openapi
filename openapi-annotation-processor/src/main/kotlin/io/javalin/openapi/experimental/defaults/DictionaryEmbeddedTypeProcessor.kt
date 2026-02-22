package io.javalin.openapi.experimental.defaults

import com.fasterxml.jackson.databind.JsonNode
import io.javalin.openapi.experimental.EmbeddedTypeProcessor
import io.javalin.openapi.experimental.EmbeddedTypeProcessorContext
import io.javalin.openapi.experimental.StructureType.DICTIONARY
import io.javalin.openapi.experimental.processor.shared.createObjectNode

class DictionaryEmbeddedTypeProcessor : EmbeddedTypeProcessor {

    override fun process(context: EmbeddedTypeProcessorContext): Boolean = with (context) {
        if (type.structureType == DICTIONARY) {
            scheme.put("type", "object")
            val additionalProperties = createObjectNode()
            val additionalType = context.type.generics[1]

            context.parentContext.configuration.embeddedTypeProcessors
                .firstOrNull {
                    it.process(
                        context.copy(
                            scheme = additionalProperties,
                            type = additionalType
                        )
                    )
                }
                ?: parentContext.typeSchemaGenerator.addType(
                    scheme = additionalProperties,
                    type = additionalType,
                    inlineRefs = inlineRefs,
                    references = references,
                    requiresNonNulls = requiresNonNulls
                )

            scheme.set<JsonNode>("additionalProperties", additionalProperties)
            return true
        }

        return false
    }

}
