package io.javalin.openapi.experimental.processor.generators

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.introspection.Accessor
import io.javalin.introspection.AnnotationValue
import io.javalin.introspection.AnnotationSet
import io.javalin.introspection.ClassDefinition as RawType
import io.javalin.introspection.InternalIntrospectionApi
import io.javalin.introspection.MemberVisibility
import io.javalin.openapi.*
import io.javalin.openapi.experimental.*
import io.javalin.openapi.experimental.processor.shared.*

class TypeSchemaGenerator(val context: SchemaGenerationContext) {

    // The cache helps to avoid processing the same property multiple times & prevent infinite recursion
    // ~ https://github.com/javalin/javalin-openapi/issues/230
    private val processedProperties = mutableMapOf<ProcessedProperty, ResultScheme>()

    private data class ProcessedProperty(
        val property: Property,
        val inlineRefs: Boolean,
        val requiresNonNulls: Boolean,
    )

    private fun OpenApiType.definedBy(): OpenApiType? =
        context.annotationsOf(this)
            .find(OpenApiPropertyType::class.java)
            ?.get("definedBy")
            ?.asClassDefinition()
            ?.let(context::toOpenApiType)

    fun createTypeSchema(
        type: OpenApiType,
        inlineRefs: Boolean = false,
        requireNonNullsByDefault: Boolean = true,
    ): ResultScheme {
        context.reportDebug("OpenApi | Generating schema for ${type.fullName}")

        val annotations = context.annotationsOf(type)
        val isEnum = context.isEnum(type)
        val definedBy = type.definedBy()

        if (definedBy != null && !isEnum) {
            return createTypeSchema(
                type = definedBy,
                inlineRefs = inlineRefs,
                requireNonNullsByDefault = requireNonNullsByDefault,
            )
        }

        val schema = createObjectNode()
        val references = mutableSetOf<OpenApiType>()
        val composition = findCompositionInElement(context, annotations)

        when {
            composition != null -> {
                schema.createComposition(
                    context = context,
                    type = type,
                    propertyComposition = composition,
                    references = references,
                    inlineRefs = inlineRefs,
                    requiresNonNulls = requireNonNullsByDefault,
                )
            }
            isEnum -> {
                val enumType = definedBy
                    ?.let { context.simpleTypeMappings[it.fullName] }

                val namingStrategy = annotations.namingStrategy()
                val values = createArrayNode()
                val descriptions = createArrayNode()

                for (constant in context.enumConstantsOf(type)) {
                    val customName = constant.annotations.find(OpenApiName::class.java)?.get("value")?.asString()
                    val description = constant.annotations.find(OpenApiDescription::class.java)?.get("value")?.asString()
                    val name = when {
                        customName != null -> customName
                        namingStrategy != null -> translatePropertyName(namingStrategy, constant.name)
                        else -> constant.name
                    }

                    when {
                        enumType != null && enumType.type != "string" -> values.add(jsonMapper.readTree(name))
                        else -> values.add(name)
                    }
                    descriptions.add(description ?: "")
                }

                schema.put("type", enumType?.type ?: "string")
                enumType?.format?.also { schema.put("format", it) }
                schema.set<JsonNode>("enum", values)

                if (descriptions.any { it.isTextual && it.asText().isNotEmpty() }) {
                    schema.set<JsonNode>("x-enum-descriptions", descriptions)
                }

                schema.addExtra(annotations.findExtra())
            }
            else -> {
                schema.put("type", "object")

                schema.addExtra(annotations.findExtra())

                val propertiesObject = createObjectNode()
                schema.set<JsonNode>("properties", propertiesObject)

                val requireNonNulls = (annotations.find(JsonSchema::class.java)?.get("requireNonNulls")?.asBoolean())
                    ?: requireNonNullsByDefault

                val properties = context.findAllProperties(type, requireNonNulls)

                properties.forEach { property ->
                    val processedProperty = ProcessedProperty(
                        property = property,
                        inlineRefs = inlineRefs,
                        requiresNonNulls = requireNonNulls,
                    )
                    val result = processedProperties.getOrPut(processedProperty) {
                        createEmbeddedTypeDescription(
                            type = property.type,
                            inlineRefs = inlineRefs,
                            requiresNonNulls = requireNonNulls,
                            composition = property.composition,
                            extra = property.extra,
                            nullable = property.nullable,
                        )
                    }
                    propertiesObject.set<JsonNode>(property.name, result.json)
                    result.references.forEach { references.addReference(it) }
                }

                if (properties.any { it.required }) {
                    val required = createArrayNode()
                    properties.filter { it.required }.forEach { required.add(it.name) }
                    schema.set<JsonNode>("required", required)
                }
            }
        }

        return ResultScheme(json = schema, references = references)
    }

    fun createEmbeddedTypeDescription(
        type: OpenApiType,
        inlineRefs: Boolean = false,
        requiresNonNulls: Boolean = true,
        composition: PropertyComposition? = null,
        extra: Map<String, Any?> = emptyMap(),
        nullable: Boolean = false,
    ): ResultScheme {
        val definedBy = type.definedBy()

        if (definedBy != null && !context.isEnum(type)) {
            return createEmbeddedTypeDescription(
                type = definedBy,
                inlineRefs = inlineRefs,
                requiresNonNulls = requiresNonNulls,
                composition = composition,
                extra = extra,
                nullable = nullable,
            )
        }

        val scheme = createObjectNode()
        val references = mutableSetOf<OpenApiType>()

        val processorContext = EmbeddedTypeProcessorContext(
            parentContext = context,
            scheme = scheme,
            references = references,
            type = type,
            inlineRefs = inlineRefs,
            requiresNonNulls = requiresNonNulls,
            composition = composition,
            extra = extra,
        )
        val handled = context.embeddedTypeProcessors.any { processor ->
            processor.process(processorContext)
        }

        if (!handled) {
            if (type.fullName == "java.util.Optional" && type.generics.size == 1) {
                return createEmbeddedTypeDescription(
                    type = type.generics.first(),
                    inlineRefs = inlineRefs,
                    requiresNonNulls = requiresNonNulls,
                    composition = composition,
                    extra = extra,
                    nullable = true,
                )
            }

            addType(
                scheme = scheme,
                type = type,
                inlineRefs = inlineRefs,
                references = references,
                requiresNonNulls = requiresNonNulls,
            )
        }

        scheme.addExtra(extra)

        if (nullable) {
            val currentType = scheme.get("type")?.takeIf { it.isTextual }?.asText()
            val currentRef = scheme.get($$"$ref")?.asText()
            val compositionKey = listOf("oneOf", "anyOf", "allOf").firstOrNull { scheme.has(it) }
            when {
                currentType != null -> {
                    scheme.remove("type")
                    scheme.set("type", createArrayNode().add(currentType).add("null"))
                }
                currentRef != null -> {
                    scheme.remove($$"$ref")
                    val anyOf = createArrayNode()
                    anyOf.add(createObjectNode().put($$"$ref", currentRef))
                    anyOf.add(createObjectNode().put("type", "null"))
                    scheme.set("anyOf", anyOf)
                }
                compositionKey == "allOf" -> {
                    val allOfArray = scheme.remove("allOf")
                    val discriminator = scheme.remove("discriminator")
                    val inner = createObjectNode()
                    inner.set<JsonNode>("allOf", allOfArray)
                    if (discriminator != null) inner.set<JsonNode>("discriminator", discriminator)
                    val anyOf = createArrayNode()
                    anyOf.add(inner)
                    anyOf.add(createObjectNode().put("type", "null"))
                    scheme.set("anyOf", anyOf)
                }
                compositionKey != null -> {
                    (scheme.get(compositionKey) as? ArrayNode)?.add(createObjectNode().put("type", "null"))
                }
            }
        }

        return ResultScheme(json = scheme, references = references)
    }

    fun addType(
        scheme: ObjectNode,
        type: OpenApiType,
        inlineRefs: Boolean,
        references: MutableSet<OpenApiType>,
        requiresNonNulls: Boolean,
    ) {
        when (val nonRefType = context.simpleTypeMappings[type.fullName]) {
            null -> {
                when {
                    inlineRefs -> {
                        val (subScheme, subReferences) = createTypeSchema(
                            type = type,
                            inlineRefs = true,
                            requireNonNullsByDefault = requiresNonNulls,
                        )
                        subScheme.properties().forEach { (key, value) -> scheme.set<JsonNode>(key, value) }
                        subReferences.forEach { references.addReference(it) }
                    }
                    else -> {
                        references.addReference(type)
                        scheme.put($$"$ref", "#/components/schemas/${type.simpleName}")
                    }
                }
            }
            else -> {
                scheme.put("type", nonRefType.type)
                nonRefType.format?.also { scheme.put("format", it) }
            }
        }
    }

}

internal fun SchemaGenerationContext.findAllProperties(type: OpenApiType, requireNonNulls: Boolean): Collection<Property> {
    val annotations = annotationsOf(type)
    val byFields = annotations.find(OpenApiByFields::class.java)
    val byFieldsOnly = byFields?.get("only")?.asBoolean() == true
    val byFieldsVisibility = byFields?.get("value")?.asString()?.let { Visibility.valueOf(it) }
    val namingStrategy = annotations.namingStrategy()

    val declaredProperties = mutableListOf<Property>()

    for (property in propertiesOf(type)) {
        if (!acceptsProperty(type, property)) continue

        when (property.accessor) {
            Accessor.FIELD -> if (byFields == null) continue
            Accessor.GETTER -> if (byFieldsOnly) continue
            Accessor.RECORD_COMPONENT -> {}
        }
        if (byFieldsVisibility != null && byFieldsVisibility.priority > property.visibility.toOpenApi().priority) continue
        if (property.annotations.contains(OpenApiIgnore::class.java) || property.transient) continue

        val customName = property.annotations.find(OpenApiName::class.java)?.get("value")?.asString()
        val name = customName ?: property.name
        val finalName = when {
            customName == null && namingStrategy != null -> translatePropertyName(namingStrategy, name)
            else -> name
        }

        val propertyType = property.annotations.find(OpenApiPropertyType::class.java)
        val nullability = propertyType?.get("nullability")?.asString()
        val redirect = propertyType?.get("definedBy")?.asClassDefinition()
        val treatedAsNotNull = (redirect == null && !property.nullable) || redirect?.hasPrimitiveSource() == true

        val isNotNull = when {
            nullability == Nullability.NOT_NULL.name -> true
            nullability == Nullability.NULLABLE.name -> false
            property.annotations.contains("NotNull") -> true
            treatedAsNotNull -> true
            property.annotations.contains("Nullable") -> false
            else -> false
        }
        val required = property.annotations.contains(OpenApiRequired::class.java) || (requireNonNulls && isNotNull)

        val explicitNullable = property.annotations.find(OpenApiNullable::class.java)?.get("nullable")?.asBoolean()
        val isExplicitlyNullable = when {
            explicitNullable != null -> explicitNullable
            nullability == Nullability.NULLABLE.name -> true
            property.annotations.contains("Nullable") -> true
            else -> false
        }

        declaredProperties.add(
            Property(
                name = finalName,
                type = toOpenApiType(redirect ?: property.type),
                composition = findCompositionInElement(this, property.annotations),
                required = required,
                nullable = isExplicitlyNullable,
                extra = property.annotations.findExtra(),
            )
        )
    }

    val customProperties =
        type
            .extra
            .filterIsInstance<CustomProperty>()
            .map { extraProperty ->
                Property(
                    name = extraProperty.name,
                    type = extraProperty.type,
                    required = requireNonNulls,
                )
            }
    val properties = declaredProperties + customProperties

    reportDebug("OpenApi | Resolved ${properties.size} properties for ${type.fullName}: ${properties.joinToString { it.name }}")

    return properties
}

private fun AnnotationSet.namingStrategy(): OpenApiNamingStrategy? =
    (find(OpenApiNaming::class.java)?.get("value")?.asString())?.let { OpenApiNamingStrategy.valueOf(it) }

private fun AnnotationSet.findExtra(): Map<String, Any?> {
    val extra = mutableMapOf<String, Any?>(
        "description" to find(OpenApiDescription::class.java)?.get("value")?.asString()
    )

    find(OpenApiExample::class.java)?.also { example ->
        val value = example["value"].notNullString()
        val raw = example["raw"].notNullString()
        val objects = example["objects"].asList().filterIsInstance<Map<String, Any?>>()
        when {
            value != null -> extra["example"] = value
            raw != null -> extra["example"] = jsonMapper.readTree(raw)
            objects.isNotEmpty() -> {
                val result = ExampleGenerator.generateFromExamples(objects.map { it.toExampleProperty() })
                extra["example"] = result.jsonElement ?: result.simpleValue
            }
        }
    }

    find(OpenApiNumberValidation::class.java)?.also { validation ->
        extra["minimum"] = validation["minimum"].notNullString()?.toBigDecimal()
        extra["maximum"] = validation["maximum"].notNullString()?.toBigDecimal()
        extra["exclusiveMinimum"] = validation["exclusiveMinimum"].notNullString()?.toBigDecimal()
        extra["exclusiveMaximum"] = validation["exclusiveMaximum"].notNullString()?.toBigDecimal()
        extra["multipleOf"] = validation["multipleOf"].notNullString()?.toBigDecimal()
    }

    find(OpenApiStringValidation::class.java)?.also { validation ->
        extra["minLength"] = validation["minLength"].notNullString()?.toInt()
        extra["maxLength"] = validation["maxLength"].notNullString()?.toInt()
        extra["format"] = validation["format"].notNullString()
        extra["pattern"] = validation["pattern"].notNullString()
    }

    find(OpenApiArrayValidation::class.java)?.also { validation ->
        extra["minItems"] = validation["minItems"].notNullString()?.toInt()
        extra["maxItems"] = validation["maxItems"].notNullString()?.toInt()
        extra["uniqueItems"] = validation["uniqueItems"].asBoolean()?.takeIf { unique -> unique }
    }

    find(OpenApiObjectValidation::class.java)?.also { validation ->
        extra["minProperties"] = validation["minProperties"].notNullString()?.toInt()
        extra["maxProperties"] = validation["maxProperties"].notNullString()?.toInt()
    }

    findAll(Custom::class.java).forEach { custom ->
        extra[requireNotNull(custom.get("name").asString())] = custom.get("value").raw()
    }

    all()
        .filter { it.metadata.contains(CustomAnnotation::class.java) }
        .flatMap { it.values.entries }
        .forEach { (name, value) -> extra[name] = customAnnotationValue(value) }

    return extra
}

private fun MemberVisibility.toOpenApi(): Visibility =
    when (this) {
        MemberVisibility.PUBLIC -> Visibility.PUBLIC
        MemberVisibility.PROTECTED -> Visibility.PROTECTED
        MemberVisibility.PRIVATE -> Visibility.PRIVATE
        MemberVisibility.PACKAGE_PRIVATE -> Visibility.DEFAULT
    }

private fun AnnotationValue.notNullString(): String? =
    asString()?.takeIf { it != NULL_STRING }

private val primitiveSourceNames = setOf(
    "boolean", "byte", "short", "int", "long", "float", "double", "char",
    "Boolean", "Byte", "Short", "Int", "Long", "Float", "Double", "Char",
    "kotlin.Boolean", "kotlin.Byte", "kotlin.Short", "kotlin.Int", "kotlin.Long", "kotlin.Float", "kotlin.Double", "kotlin.Char",
)

@OptIn(InternalIntrospectionApi::class)
private fun RawType.hasPrimitiveSource(): Boolean =
    source.toString() in primitiveSourceNames

private fun customAnnotationValue(value: Any?): Any? =
    when (value) {
        is String -> value.trimIndent()
        is RawType -> value.fullName
        is Map<*, *> -> createObjectNode().also { node ->
            value.forEach { (key, nestedValue) ->
                val field = key as? String ?: return@forEach
                when (val resolved = customAnnotationValue(nestedValue)) {
                    is Boolean -> node.put(field, resolved)
                    is Int -> node.put(field, resolved)
                    is Long -> node.put(field, resolved)
                    is Double -> node.put(field, resolved)
                    is Float -> node.put(field, resolved)
                    is Short -> node.put(field, resolved.toInt())
                    is Byte -> node.put(field, resolved.toInt())
                    is String -> node.put(field, resolved)
                    is JsonNode -> node.set<JsonNode>(field, resolved)
                    null -> {}
                    else -> node.put(field, resolved.toString())
                }
            }
        }
        is List<*> -> createArrayNode().also { array ->
            value.forEach {
                when (val element = customAnnotationValue(it)) {
                    is Boolean -> array.add(element)
                    is Int -> array.add(element)
                    is Long -> array.add(element)
                    is Double -> array.add(element)
                    is Float -> array.add(element)
                    is Short -> array.add(element.toInt())
                    is Byte -> array.add(element.toInt())
                    is String -> array.add(element)
                    is JsonNode -> array.add(element)
                    else -> throw UnsupportedOperationException("[CustomAnnotation] Unsupported array value: $it")
                }
            }
        }
        else -> value
    }
