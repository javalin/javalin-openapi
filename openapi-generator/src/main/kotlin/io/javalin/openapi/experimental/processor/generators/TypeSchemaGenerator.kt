package io.javalin.openapi.experimental.processor.generators

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.introspection.Accessor
import io.javalin.introspection.Annotations
import io.javalin.introspection.Visibility as RawVisibility
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
        val definedBy = annotations.resolveClass(OpenApiPropertyType::class.java, "definedBy")?.let { context.toOpenApiType(it) }

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

                context.enumConstantsOf(type)
                    .map { constant ->
                        val customName = constant.annotations.memberValues(OpenApiName::class.java)?.get("value") as? String
                        when {
                            customName != null -> customName
                            namingStrategy != null -> translatePropertyName(namingStrategy, constant.name)
                            else -> constant.name
                        }
                    }
                    .forEach { name ->
                        if (enumType != null && enumType.type != "string") {
                            values.add(jsonMapper.readTree(name))
                        } else {
                            values.add(name)
                        }
                    }

                schema.put("type", enumType?.type ?: "string")
                enumType?.format?.also { schema.put("format", it) }
                schema.set<JsonNode>("enum", values)

                schema.addExtra(annotations.findExtra())
            }
            else -> {
                schema.put("type", "object")

                schema.addExtra(annotations.findExtra())

                val propertiesObject = createObjectNode()
                schema.set<JsonNode>("properties", propertiesObject)

                val requireNonNulls = (annotations.memberValues(JsonSchema::class.java)?.get("requireNonNulls") as? Boolean)
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
        val definedBy = context.annotationsOf(type).resolveClass(OpenApiPropertyType::class.java, "definedBy")?.let { context.toOpenApiType(it) }

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
            // Unwrap Optional<T> as nullable T
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
    val byFields = annotations.memberValues(OpenApiByFields::class.java)
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
        if (property.annotations.has(OpenApiIgnore::class.java) || property.transient) continue

        val customName = property.annotations.memberValues(OpenApiName::class.java)?.get("value") as? String
        val name = customName ?: property.name
        val finalName = if (customName == null && namingStrategy != null) translatePropertyName(namingStrategy, name) else name

        val nullability = property.annotations.memberValues(OpenApiPropertyType::class.java)?.get("nullability") as? String
        val redirect = property.annotations.resolveClass(OpenApiPropertyType::class.java, "definedBy")
        val isPrimitive = redirect == null && !property.nullable

        val isNotNull = when {
            nullability == Nullability.NOT_NULL.name -> true
            nullability == Nullability.NULLABLE.name -> false
            property.annotations.hasNamed("NotNull") -> true
            isPrimitive -> true
            property.annotations.hasNamed("Nullable") -> false
            else -> false
        }
        val required = property.annotations.has(OpenApiRequired::class.java) || (requireNonNulls && isNotNull)

        val explicitNullable = property.annotations.memberValues(OpenApiNullable::class.java)?.get("nullable") as? Boolean
        val isExplicitlyNullable = when {
            explicitNullable != null -> explicitNullable
            nullability == Nullability.NULLABLE.name -> true
            property.annotations.hasNamed("Nullable") -> true
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

private fun Annotations.namingStrategy(): OpenApiNamingStrategy? =
    (memberValues(OpenApiNaming::class.java)?.get("value") as? String)?.let { OpenApiNamingStrategy.valueOf(it) }

private fun Annotations.findExtra(): Map<String, Any?> {
    val extra = mutableMapOf<String, Any?>(
        "description" to memberValues(OpenApiDescription::class.java)?.get("value")
    )

    memberValues(OpenApiExample::class.java)?.also { example ->
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

    memberValues(OpenApiNumberValidation::class.java)?.also {
        extra["minimum"] = it.notNull("minimum")?.toBigDecimal()
        extra["maximum"] = it.notNull("maximum")?.toBigDecimal()
        extra["exclusiveMinimum"] = it.notNull("exclusiveMinimum")?.toBigDecimal()
        extra["exclusiveMaximum"] = it.notNull("exclusiveMaximum")?.toBigDecimal()
        extra["multipleOf"] = it.notNull("multipleOf")?.toBigDecimal()
    }

    memberValues(OpenApiStringValidation::class.java)?.also {
        extra["minLength"] = it.notNull("minLength")?.toInt()
        extra["maxLength"] = it.notNull("maxLength")?.toInt()
        extra["format"] = it.notNull("format")
        extra["pattern"] = it.notNull("pattern")
    }

    memberValues(OpenApiArrayValidation::class.java)?.also {
        extra["minItems"] = it.notNull("minItems")?.toInt()
        extra["maxItems"] = it.notNull("maxItems")?.toInt()
        extra["uniqueItems"] = (it["uniqueItems"] as? Boolean)?.takeIf { unique -> unique }
    }

    memberValues(OpenApiObjectValidation::class.java)?.also {
        extra["minProperties"] = it.notNull("minProperties")?.toInt()
        extra["maxProperties"] = it.notNull("maxProperties")?.toInt()
    }

    memberValuesList(Custom::class.java).forEach { custom ->
        extra[custom["name"] as String] = custom["value"]
    }

    all()
        .filter { it.meta.has(CustomAnnotation::class.java) }
        .flatMap { it.values().entries }
        .forEach { (name, value) -> extra[name] = customAnnotationValue(value) }

    return extra
}

private fun RawVisibility.toOpenApi(): Visibility =
    when (this) {
        RawVisibility.PUBLIC -> Visibility.PUBLIC
        RawVisibility.PROTECTED -> Visibility.PROTECTED
        RawVisibility.PRIVATE -> Visibility.PRIVATE
        RawVisibility.PACKAGE_PRIVATE -> Visibility.DEFAULT
    }

private fun Map<String, Any?>.notNull(key: String): String? =
    (this[key] as? String)?.takeIf { it != NULL_STRING }

private fun customAnnotationValue(value: Any?): Any? =
    when (value) {
        is String -> value.trimIndent()
        is io.javalin.introspection.ClassDefinition -> value.fullName
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
