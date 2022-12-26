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
            parentContext.typeSchemaGenerator.addType(additionalProperties, context.type.generics[1], inlineRefs, references, requiresNonNulls)
            scheme.add("additionalProperties", additionalProperties)
            return true
        }

        return false
    }

}