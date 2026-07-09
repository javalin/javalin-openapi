package io.javalin.openapi.experimental.processor.generators

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.introspection.Accessor
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
    private val processedProperties = mutableMapOf<Property, ResultScheme>()

    fun createTypeSchema(
        type: OpenApiType,
        inlineRefs: Boolean = false,
        requireNonNullsByDefault: Boolean = true
    ): ResultScheme {
        val annotations = context.annotationsOf(type)
        val isEnum = context.isEnum(type)
        val definedBy = annotations.find(OpenApiPropertyType::class.java)?.classValue("definedBy")?.let { context.toOpenApiType(it) }

        if (definedBy != null && !isEnum) {
            return createTypeSchema(definedBy, inlineRefs, requireNonNullsByDefault)
        }

        val schema = createObjectNode()
        val references = mutableSetOf<OpenApiType>()
        val composition = findCompositionInElement(context, annotations)

        when {
            composition != null -> {
                schema.createComposition(context, type, composition, references, inlineRefs, requireNonNullsByDefault)
            }
            isEnum -> {
                val enumType = definedBy
                    ?.let { context.simpleTypeMappings[it.fullName] }

                val namingStrategy = annotations.namingStrategy()
                val values = createArrayNode()
                val descriptions = createArrayNode()

                context.enumConstantsOf(type)
                    .map { constant ->
                        val customName = constant.annotations.find(OpenApiName::class.java)?.string("value")
                        val description = constant.annotations.find(OpenApiDescription::class.java)?.string("value")
                        val name = when {
                            customName != null -> customName
                            namingStrategy != null -> translatePropertyName(namingStrategy, constant.name)
                            else -> constant.name
                        }
                        name to (description ?: "")
                    }
                    .forEach { (name, description) ->
                        if (enumType != null && enumType.type != "string") {
                            values.add(jsonMapper.readTree(name))
                        } else {
                            values.add(name)
                        }
                        descriptions.add(description)
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

                val requireNonNulls = (annotations.find(JsonSchema::class.java)?.boolean("requireNonNulls"))
                    ?: requireNonNullsByDefault

                val properties = context.findAllProperties(type, requireNonNulls)

                properties.forEach { property ->
                    val result =
                        when {
                            processedProperties.contains(property) ->
                                processedProperties[property]!!
                            else ->
                                createEmbeddedTypeDescription(
                                    type = property.type,
                                    inlineRefs = inlineRefs,
                                    requiresNonNulls = requireNonNulls,
                                    composition = property.composition,
                                    extra = property.extra,
                                    nullable = property.nullable,
                                ).also {
                                    processedProperties[property] = it
                                }
                        }
                    propertiesObject.set<JsonNode>(property.name, result.json)
                    references.addAll(result.references)
                }

                if (properties.any { it.required }) {
                    val required = createArrayNode()
                    properties.filter { it.required }.forEach { required.add(it.name) }
                    schema.set<JsonNode>("required", required)
                }
            }
        }

        return ResultScheme(schema, references)
    }

    fun createEmbeddedTypeDescription(
        type: OpenApiType,
        inlineRefs: Boolean = false,
        requiresNonNulls: Boolean = true,
        composition: PropertyComposition? = null,
        extra: Map<String, Any?> = emptyMap(),
        nullable: Boolean = false,
    ): ResultScheme {
        val definedBy = context.annotationsOf(type).find(OpenApiPropertyType::class.java)?.classValue("definedBy")?.let { context.toOpenApiType(it) }

        if (definedBy != null && !context.isEnum(type)) {
            return createEmbeddedTypeDescription(definedBy, inlineRefs, requiresNonNulls, composition, extra, nullable)
        }

        val scheme = createObjectNode()
        val references = mutableSetOf<OpenApiType>()

        val handledByCustomProcessor =
            context.embeddedTypeProcessors.firstOrNull {
                it.process(
                    EmbeddedTypeProcessorContext(
                        parentContext = context,
                        scheme = scheme,
                        references = references,
                        type = type,
                        inlineRefs = inlineRefs,
                        requiresNonNulls = requiresNonNulls,
                        composition = composition,
                        extra = extra
                    )
                )
            }

        if (handledByCustomProcessor == null) {
            if (type.fullName == "java.util.Optional" && type.generics.size == 1) {
                return createEmbeddedTypeDescription(type.generics.first(), inlineRefs, requiresNonNulls, composition, extra, nullable = true)
            }

            addType(scheme, type, inlineRefs, references, requiresNonNulls)
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

        return ResultScheme(scheme, references)
    }

    fun addType(
        scheme: ObjectNode,
        type: OpenApiType,
        inlineRefs: Boolean,
        references: MutableSet<OpenApiType>,
        requiresNonNulls: Boolean
    ) {
        when (val nonRefType = context.simpleTypeMappings[type.fullName]) {
            null -> {
                if (inlineRefs) {
                    val (subScheme, subReferences) = createTypeSchema(type, true, requiresNonNulls)
                    subScheme.properties().forEach { (key, value) -> scheme.set<JsonNode>(key, value) }
                    references.addAll(subReferences)
                } else {
                    references.add(type)
                    scheme.put($$"$ref", "#/components/schemas/${type.simpleName}")
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
    val byFields = annotations.find(OpenApiByFields::class.java)?.values
    val byFieldsOnly = byFields?.get("only") == true
    val byFieldsVisibility = (byFields?.get("value") as? String)?.let { Visibility.valueOf(it) }
    val namingStrategy = annotations.namingStrategy()

    val properties = mutableListOf<Property>()

    for (property in propertiesOf(type)) {
        if (!acceptsProperty(type, property)) continue

        when (property.accessor) {
            Accessor.FIELD -> if (byFields == null) continue
            Accessor.GETTER -> if (byFieldsOnly) continue
            Accessor.RECORD_COMPONENT -> {}
        }
        if (byFieldsVisibility != null && byFieldsVisibility.priority > property.visibility.toOpenApi().priority) continue
        if (property.annotations.contains(OpenApiIgnore::class.java) || property.transient) continue

        val customName = property.annotations.find(OpenApiName::class.java)?.string("value")
        val name = customName ?: property.name
        val finalName = if (customName == null && namingStrategy != null) translatePropertyName(namingStrategy, name) else name

        val nullability = property.annotations.find(OpenApiPropertyType::class.java)?.string("nullability")
        val redirect = property.annotations.find(OpenApiPropertyType::class.java)?.classValue("definedBy")
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

        val explicitNullable = property.annotations.find(OpenApiNullable::class.java)?.boolean("nullable")
        val isExplicitlyNullable = when {
            explicitNullable != null -> explicitNullable
            nullability == Nullability.NULLABLE.name -> true
            property.annotations.contains("Nullable") -> true
            else -> false
        }

        properties.add(
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

    type.extra
        .filterIsInstance<CustomProperty>()
        .forEach { extraProperty ->
            properties.add(
                Property(
                    name = extraProperty.name,
                    type = extraProperty.type,
                    required = requireNonNulls
                )
            )
        }

    return properties
}

private fun AnnotationSet.namingStrategy(): OpenApiNamingStrategy? =
    (find(OpenApiNaming::class.java)?.string("value"))?.let { OpenApiNamingStrategy.valueOf(it) }

private fun AnnotationSet.findExtra(): Map<String, Any?> {
    val extra = mutableMapOf<String, Any?>(
        "description" to find(OpenApiDescription::class.java)?.value("value")
    )

    find(OpenApiExample::class.java)?.values?.also { example ->
        val value = example.notNull("value")
        val raw = example.notNull("raw")
        val objects = (example["objects"] as? List<*>)?.filterIsInstance<Map<String, Any?>>().orEmpty()
        when {
            value != null -> extra["example"] = value
            raw != null -> extra["example"] = jsonMapper.readTree(raw)
            objects.isNotEmpty() -> {
                val result = ExampleGenerator.generateFromExamples(objects.map { it.toExampleProperty() })
                extra["example"] = result.jsonElement ?: result.simpleValue
            }
        }
    }

    find(OpenApiNumberValidation::class.java)?.values?.also {
        extra["minimum"] = it.notNull("minimum")?.toBigDecimal()
        extra["maximum"] = it.notNull("maximum")?.toBigDecimal()
        extra["exclusiveMinimum"] = it.notNull("exclusiveMinimum")?.toBigDecimal()
        extra["exclusiveMaximum"] = it.notNull("exclusiveMaximum")?.toBigDecimal()
        extra["multipleOf"] = it.notNull("multipleOf")?.toBigDecimal()
    }

    find(OpenApiStringValidation::class.java)?.values?.also {
        extra["minLength"] = it.notNull("minLength")?.toInt()
        extra["maxLength"] = it.notNull("maxLength")?.toInt()
        extra["format"] = it.notNull("format")
        extra["pattern"] = it.notNull("pattern")
    }

    find(OpenApiArrayValidation::class.java)?.values?.also {
        extra["minItems"] = it.notNull("minItems")?.toInt()
        extra["maxItems"] = it.notNull("maxItems")?.toInt()
        extra["uniqueItems"] = (it["uniqueItems"] as? Boolean)?.takeIf { unique -> unique }
    }

    find(OpenApiObjectValidation::class.java)?.values?.also {
        extra["minProperties"] = it.notNull("minProperties")?.toInt()
        extra["maxProperties"] = it.notNull("maxProperties")?.toInt()
    }

    findAll(Custom::class.java).forEach { custom ->
        extra[custom.value("name") as String] = custom.value("value")
    }

    all()
        .filter { it.meta.contains(CustomAnnotation::class.java) }
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

private fun Map<String, Any?>.notNull(key: String): String? =
    (this[key] as? String)?.takeIf { it != NULL_STRING }

private val primitiveSourceNames = setOf("boolean", "byte", "short", "int", "long", "float", "double", "char")

@OptIn(InternalIntrospectionApi::class)
private fun RawType.hasPrimitiveSource(): Boolean =
    source.toString() in primitiveSourceNames

private fun customAnnotationValue(value: Any?): Any? =
    when (value) {
        is String -> value.trimIndent()
        is io.javalin.introspection.ClassDefinition -> value.fullName
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
