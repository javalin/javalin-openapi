package io.javalin.openapi.experimental.defaults

import com.fasterxml.jackson.databind.JsonNode
import io.javalin.openapi.experimental.EmbeddedTypeProcessor
import io.javalin.openapi.experimental.EmbeddedTypeProcessorContext
import io.javalin.openapi.experimental.StructureType.ARRAY
import io.javalin.openapi.experimental.processor.shared.createObjectNode

class ArrayEmbeddedTypeProcessor : EmbeddedTypeProcessor {

    override fun process(context: EmbeddedTypeProcessorContext): Boolean = with(context) {
        if (type.structureType == ARRAY) {
            if (type.simpleName == "Byte") {
                scheme.put("type", "string")
                scheme.put("format", "binary")
            }
            else {
                context.scheme.put("type", "array")
                val items = createObjectNode()
                context.parentContext.typeSchemaGenerator.addType(items, type, inlineRefs, references, requiresNonNulls)
                context.scheme.set<JsonNode>("items", items)
            }

            return true
        }

        return false
    }

}
