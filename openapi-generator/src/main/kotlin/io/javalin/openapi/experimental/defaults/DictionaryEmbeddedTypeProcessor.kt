package io.javalin.openapi.experimental.defaults

import com.google.gson.JsonObject
import io.javalin.openapi.experimental.EmbeddedTypeProcessor
import io.javalin.openapi.experimental.EmbeddedTypeProcessorContext
import io.javalin.openapi.experimental.StructureType.DICTIONARY

class DictionaryEmbeddedTypeProcessor : EmbeddedTypeProcessor {

    override fun process(context: EmbeddedTypeProcessorContext): Boolean = with (context) {
        if (type.structureType == DICTIONARY) {
            scheme.addProperty("type", "object")
            val additionalProperties = JsonObject()
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

            scheme.add("additionalProperties", additionalProperties)
            return true
        }

        return false
    }

}