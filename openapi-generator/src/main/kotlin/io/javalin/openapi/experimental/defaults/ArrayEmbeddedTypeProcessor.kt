package io.javalin.openapi.experimental.defaults

import com.fasterxml.jackson.databind.JsonNode
import io.javalin.openapi.experimental.EmbeddedTypeProcessor
import io.javalin.openapi.experimental.EmbeddedTypeProcessorContext
import io.javalin.openapi.experimental.StructureType.ARRAY
import io.javalin.openapi.experimental.processor.shared.createObjectNode

class ArrayEmbeddedTypeProcessor : EmbeddedTypeProcessor {

    override fun process(context: EmbeddedTypeProcessorContext): Boolean {
        if (context.type.structureType != ARRAY) {
            return false
        }

        if (context.type.simpleName == "Byte") {
            context.scheme.put("type", "string")
            context.scheme.put("format", "binary")
            return true
        }

        context.scheme.put("type", "array")
        val items = createObjectNode()
        context.parentContext.typeSchemaGenerator.addType(
            scheme = items,
            type = context.type,
            inlineRefs = context.inlineRefs,
            references = context.references,
            requiresNonNulls = context.requiresNonNulls,
        )
        context.scheme.set<JsonNode>("items", items)
        return true
    }
}
