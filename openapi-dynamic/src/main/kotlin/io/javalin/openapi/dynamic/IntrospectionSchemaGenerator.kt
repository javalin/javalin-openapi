package io.javalin.openapi.dynamic

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.SimpleType
import io.javalin.openapi.experimental.StructureType.ARRAY
import io.javalin.openapi.experimental.StructureType.DICTIONARY
import io.javalin.openapi.experimental.TypeIntrospector
import io.javalin.openapi.experimental.defaults.createDefaultSimpleTypeMappings
import io.javalin.openapi.experimental.processor.generators.ResultScheme
import io.javalin.openapi.experimental.processor.generators.addExtra
import io.javalin.openapi.experimental.processor.shared.createArrayNode
import io.javalin.openapi.experimental.processor.shared.createObjectNode

/** Backend-agnostic JSON Schema generator driven by [TypeIntrospector] (+ `simpleTypeMappings`). */
class IntrospectionSchemaGenerator(
    private val introspector: TypeIntrospector,
    private val simpleTypeMappings: Map<String, SimpleType> = createDefaultSimpleTypeMappings(),
) {

    /** Full component schema for [type] — the body stored under `components/schemas/{simpleName}`. */
    fun createTypeSchema(type: ClassDefinition): ResultScheme {
        val schema = createObjectNode()
        val references = mutableSetOf<ClassDefinition>()

        if (introspector.isEnum(type)) {
            schema.put("type", "string")
            val values = createArrayNode()
            introspector.enumConstants(type)!!.forEach { values.add(it) }
            schema.set<JsonNode>("enum", values)
            return ResultScheme(schema, references)
        }

        schema.put("type", "object")
        val propertiesNode = createObjectNode()
        schema.set<JsonNode>("properties", propertiesNode)

        val properties = introspector.properties(type)
        for (property in properties) {
            val result = createEmbeddedTypeDescription(property.type, property.extra)
            propertiesNode.set<JsonNode>(property.name, result.json)
            references.addAll(result.references)
        }

        val required = properties.filter { it.required }
        if (required.isNotEmpty()) {
            val requiredNode = createArrayNode()
            required.forEach { requiredNode.add(it.name) }
            schema.set<JsonNode>("required", requiredNode)
        }

        return ResultScheme(schema, references)
    }

    /** Inline, structure-aware description for a property/value: `$ref`, array, dictionary, or simple type. */
    fun createEmbeddedTypeDescription(type: ClassDefinition, extra: Map<String, Any?> = emptyMap()): ResultScheme {
        val scheme = createObjectNode()
        val references = mutableSetOf<ClassDefinition>()

        when (type.structureType) {
            ARRAY ->
                if (type.simpleName == "Byte") {
                    scheme.put("type", "string")
                    scheme.put("format", "binary")
                } else {
                    scheme.put("type", "array")
                    val items = createObjectNode()
                    addType(items, type, references)
                    scheme.set<JsonNode>("items", items)
                }
            DICTIONARY -> {
                scheme.put("type", "object")
                val additionalProperties = createEmbeddedTypeDescription(type.generics[1])
                references.addAll(additionalProperties.references)
                scheme.set("additionalProperties", additionalProperties.json)
            }
            else ->
                addType(scheme, type, references)
        }

        scheme.addExtra(extra)
        return ResultScheme(scheme, references)
    }

    private fun addType(scheme: ObjectNode, type: ClassDefinition, references: MutableSet<ClassDefinition>) {
        when (val mapped = simpleTypeMappings[type.fullName]) {
            null -> {
                references.add(type)
                scheme.put($$"$ref", "#/components/schemas/${type.simpleName}")
            }
            else -> {
                scheme.put("type", mapped.type)
                mapped.format?.also { scheme.put("format", it) }
            }
        }
    }
}
