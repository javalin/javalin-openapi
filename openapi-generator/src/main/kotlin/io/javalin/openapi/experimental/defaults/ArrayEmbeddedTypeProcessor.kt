package io.javalin.openapi.experimental.defaults

import com.google.gson.JsonObject
import io.javalin.openapi.experimental.EmbeddedTypeProcessor
import io.javalin.openapi.experimental.EmbeddedTypeProcessorContext
import io.javalin.openapi.experimental.StructureType.ARRAY

class ArrayEmbeddedTypeProcessor : EmbeddedTypeProcessor {

    override fun process(context: EmbeddedTypeProcessorContext): Boolean = with(context) {
        if (type.structureType == ARRAY) {
            if (type.simpleName == "Byte") {
                scheme.addProperty("type", "string")
                scheme.addProperty("format", "binary")
            }
            else {
                context.scheme.addProperty("type", "array")
                val items = JsonObject()
                context.parentContext.typeSchemaGenerator.addType(items, type, inlineRefs, references, requiresNonNulls)
                context.scheme.add("items", items)
            }

            return true
        }

        return false
    }

}