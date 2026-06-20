package io.javalin.openapi.experimental

import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.openapi.experimental.processor.generators.PropertyComposition

data class EmbeddedTypeProcessorContext(
    val parentContext: SchemaGenerationContext,
    val scheme: ObjectNode,
    val references: MutableSet<ClassDefinition>,
    val type: ClassDefinition,
    val inlineRefs: Boolean = false,
    val requiresNonNulls: Boolean = true,
    val composition: PropertyComposition? = null,
    val extra: Map<String, Any?> = emptyMap()
)

fun interface EmbeddedTypeProcessor {
    fun process(context: EmbeddedTypeProcessorContext): Boolean
}
