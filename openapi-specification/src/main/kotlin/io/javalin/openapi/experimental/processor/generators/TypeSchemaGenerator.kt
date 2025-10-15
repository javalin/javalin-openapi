package io.javalin.openapi.experimental.processor.generators

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.javalin.openapi.Custom
import io.javalin.openapi.CustomAnnotation
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.NULL_STRING
import io.javalin.openapi.Nullability
import io.javalin.openapi.OpenApiArrayValidation
import io.javalin.openapi.OpenApiByFields
import io.javalin.openapi.OpenApiDescription
import io.javalin.openapi.OpenApiExample
import io.javalin.openapi.OpenApiIgnore
import io.javalin.openapi.OpenApiName
import io.javalin.openapi.OpenApiNumberValidation
import io.javalin.openapi.OpenApiObjectValidation
import io.javalin.openapi.OpenApiPropertyType
import io.javalin.openapi.OpenApiRequired
import io.javalin.openapi.OpenApiStringValidation
import io.javalin.openapi.Visibility
import io.javalin.openapi.experimental.AnnotationProcessorContext
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.CustomProperty
import io.javalin.openapi.experimental.EmbeddedTypeProcessorContext
import io.javalin.openapi.experimental.processor.generators.ExampleGenerator.toExampleProperty
import io.javalin.openapi.experimental.processor.shared.MessagerWriter
import io.javalin.openapi.experimental.processor.shared.getTypeMirror
import io.javalin.openapi.experimental.processor.shared.hasAnnotation
import io.javalin.openapi.experimental.processor.shared.info
import io.javalin.openapi.experimental.processor.shared.isPrimitive
import io.javalin.openapi.experimental.processor.shared.objectType
import io.javalin.openapi.experimental.processor.shared.recordType
import io.javalin.openapi.experimental.processor.shared.toSimpleName
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.AnnotationValueVisitor
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.*
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

data class ResultScheme(
    val json: JsonObject,
    val references: Set<ClassDefinition>
)

class TypeSchemaGenerator(val context: AnnotationProcessorContext) {

    // The cache helps to avoid processing the same property multiple times & prevent infinite recursion
    // ~ https://github.com/javalin/javalin-openapi/issues/230
    private val processedProperties = mutableMapOf<Property, ResultScheme>()

    fun createTypeSchema(
        type: ClassDefinition,
        inlineRefs: Boolean = false,
        requireNonNullsByDefault: Boolean = true
    ): ResultScheme = with (context) {
        val source = type.source
        val definedBy = source.getAnnotation(OpenApiPropertyType::class.java)?.getClassDefinition { definedBy }

        if (definedBy != null) {
            return createTypeSchema(definedBy, inlineRefs, requireNonNullsByDefault)
        }

        val schema = JsonObject()
        val references = mutableSetOf<ClassDefinition>()
        val composition = findCompositionInElement(context, source)

        when {
            composition != null -> {
                schema.createComposition(context, type, composition, references, inlineRefs, requireNonNullsByDefault)
            }
            source.kind == ENUM -> {
                val values = JsonArray()
                source.enclosedElements
                    .filterIsInstance<VariableElement>()
                    .filter { it.modifiers.contains(Modifier.STATIC) }
                    .filter { context.isAssignable(it.asType(), type.mirror) }
                    .map { it.toSimpleName() }
                    .forEach { values.add(it) }

                schema.addProperty("type", "string")
                schema.add("enum", values)
            }
            else -> {
                schema.addProperty("type", "object")
                schema.addProperty("additionalProperties", false)

                val extra = source.findExtra(context)
                schema.addExtra(extra)

                val propertiesObject = JsonObject()
                schema.add("properties", propertiesObject)

                val requireNonNulls = source.getAnnotation(JsonSchema::class.java)
                    ?.requireNonNulls
                    ?: requireNonNullsByDefault

                val properties = type.findAllProperties(requireNonNulls)

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
                                    extra = property.extra
                                ).also {
                                    processedProperties[property] = it
                                }
                        }
                    propertiesObject.add(property.name, result.json)
                    references.addAll(result.references)
                }

                if (properties.any { it.required }) {
                    val required = JsonArray()
                    properties.filter { it.required }.forEach { required.add(it.name) }
                    schema.add("required", required)
                }
            }
        }

        return ResultScheme(schema, references)
    }

    fun createEmbeddedTypeDescription(
        type: ClassDefinition,
        inlineRefs: Boolean = false,
        requiresNonNulls: Boolean = true,
        composition: PropertyComposition? = null,
        extra: Map<String, Any?> = emptyMap()
    ): ResultScheme = context.inContext {
        val definedBy = type.source.getAnnotation(OpenApiPropertyType::class.java)?.getClassDefinition { definedBy }

        if (definedBy != null) {
            return@inContext createEmbeddedTypeDescription(definedBy, inlineRefs, requiresNonNulls, composition, extra)
        }

        val scheme = JsonObject()
        val references = mutableSetOf<ClassDefinition>()

        context.configuration.embeddedTypeProcessors
            .firstOrNull {
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
            ?: addType(scheme, type, inlineRefs, references, requiresNonNulls)

        scheme.addExtra(extra)
        ResultScheme(scheme, references)
    }

    fun addType(
        scheme: JsonObject,
        type: ClassDefinition,
        inlineRefs: Boolean,
        references: MutableSet<ClassDefinition>,
        requiresNonNulls: Boolean
    ) {
        when (val nonRefType = context.configuration.simpleTypeMappings[type.fullName]) {
            null -> {
                if (inlineRefs) {
                    val (subScheme, subReferences) = createTypeSchema(type, true, requiresNonNulls)
                    subScheme.asMap().forEach { (key, value) -> scheme.add(key, value) }
                    references.addAll(subReferences)
                } else {
                    references.add(type)
                    scheme.addProperty("\$ref", "#/components/schemas/${type.simpleName}")
                }
            }
            else -> {
                scheme.addProperty("type", nonRefType.type)
                nonRefType.format?.also { scheme.addProperty("format", it) }
            }
        }
    }

}

data class Property(
    val name: String,
    val type: ClassDefinition,
    val composition: PropertyComposition? = null,
    val required: Boolean = true,
    val extra: Map<String, Any?> = mutableMapOf()
)

internal fun ClassDefinition.findAllProperties(requireNonNulls: Boolean): Collection<Property> = context.inContext {
    val openApiByFields: OpenApiByFields? = source.getAnnotation(OpenApiByFields::class.java)

    val isRecord = when (recordType()) {
        null -> false
        else -> context.isAssignable(mirror, recordType()!!.asType())
    }

    inDebug { it.info("TypeSchemaGenerator#findAllProperties | Enclosed elements of ${mirror}: ${source.enclosedElements}") }
    val properties = mutableListOf<Property>()

    for (property in context.env.elementUtils.getAllMembers(context.forTypeElement(mirror))) {
        if (property is Element) {
            if (context.configuration.propertyInSchemeFilter?.filter(context, this@findAllProperties, property) == false) {
                continue
            }

            if (property.modifiers.contains(Modifier.STATIC)) {
                continue
            }

            when {
                property.kind != METHOD && openApiByFields == null -> continue
                property.kind == METHOD && openApiByFields?.only == true -> continue
            }

            val acceptFields = openApiByFields?.value

            if (acceptFields != null) {
                val modifiers = property.modifiers

                val fieldVisibility = when {
                    modifiers.contains(Modifier.PRIVATE) -> Visibility.PRIVATE
                    modifiers.contains(Modifier.PROTECTED) -> Visibility.PROTECTED
                    modifiers.contains(Modifier.DEFAULT) -> Visibility.DEFAULT
                    modifiers.contains(Modifier.PUBLIC) -> Visibility.PUBLIC
                    else -> Visibility.DEFAULT
                }

                if (acceptFields.priority > fieldVisibility.priority) {
                    continue
                }
            }

            if (property.getAnnotation(OpenApiIgnore::class.java) != null || property.modifiers.contains(Modifier.TRANSIENT)) {
                continue
            }

            if (objectType().enclosedElements.any { it.toSimpleName() == property.toSimpleName() }) {
                continue
            }

            val simpleName = property.toSimpleName()
            val customName = property.getAnnotation(OpenApiName::class.java)

            val name = when {
                customName != null -> customName.value
                isRecord || property.kind == FIELD -> simpleName
                simpleName.startsWith("get") -> simpleName.replaceFirst("get", "").replaceFirstChar { it.lowercase() }
                simpleName.startsWith("is") -> simpleName.replaceFirst("is", "").replaceFirstChar { it.lowercase() }
                else -> continue
            }

            val customType = property.getAnnotation(OpenApiPropertyType::class.java)

            val propertyType = customType?.getTypeMirror { definedBy }
                ?: (property as? ExecutableElement)?.returnType
                ?: (property as? VariableElement)?.asType()
                ?: continue

            val isNotNull = when {
                customType?.nullability == Nullability.NOT_NULL -> true
                customType?.nullability == Nullability.NULLABLE -> false
                property.hasAnnotation("NotNull") -> true
                propertyType.isPrimitive() -> true
                property.hasAnnotation("Nullable") -> false
                else -> false
            }

            val required = when {
                property.getAnnotation(OpenApiRequired::class.java) != null -> true
                else -> requireNonNulls && isNotNull
            }

            val extra = mutableMapOf<String, Any?>()

            val isExplicitlyNullable = when {
                customType?.nullability == Nullability.NULLABLE -> true
                property.hasAnnotation("Nullable") -> true
                else -> false
            }

            if (isExplicitlyNullable) {
                extra["nullable"] = true
            }

            properties.add(
                Property(
                    name = name,
                    type = propertyType.toClassDefinition(),
                    composition = findCompositionInElement(context, property),
                    required = required,
                    extra = extra + property.findExtra(context)
                )
            )
        }
    }

    extra
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

    properties
}

private fun Element.findExtra(context: AnnotationProcessorContext): Map<String, Any?> = context.inContext {
    val extra = mutableMapOf<String, Any?>(
        "description" to getAnnotation(OpenApiDescription::class.java)?.value
    )

    getAnnotationsByType(OpenApiExample::class.java).forEach { example ->
        when {
            example.value != NULL_STRING -> {
                extra["example"] = example.value
            }
            example.objects.isNotEmpty() -> {
                extra["example"] = ExampleGenerator.generateFromExamples(example.objects.map { it.toExampleProperty() })
            }
        }
    }

    getAnnotationsByType(OpenApiNumberValidation::class.java).forEach { validation ->
        extra["minimum"] = validation.minimum.takeIf { it != NULL_STRING }
        extra["maximum"] = validation.maximum.takeIf { it != NULL_STRING }
        extra["exclusiveMinimum"] = validation.exclusiveMinimum.takeIf { it }
        extra["exclusiveMaximum"] = validation.exclusiveMaximum.takeIf { it }
        extra["multipleOf"] = validation.multipleOf.takeIf { it != NULL_STRING }
    }

    getAnnotationsByType(OpenApiStringValidation::class.java).forEach { validation ->
        extra["minLength"] = validation.minLength.takeIf { it != NULL_STRING }
        extra["maxLength"] = validation.maxLength.takeIf { it != NULL_STRING }
        extra["pattern"] = validation.pattern.takeIf { it != NULL_STRING }
    }

    getAnnotationsByType(OpenApiArrayValidation::class.java).forEach { validation ->
        extra["minItems"] = validation.minItems.takeIf { it != NULL_STRING }
        extra["maxItems"] = validation.maxItems.takeIf { it != NULL_STRING }
        extra["uniqueItems"] = validation.uniqueItems.takeIf { it }
    }

    getAnnotationsByType(OpenApiObjectValidation::class.java).forEach { validation ->
        extra["minProperties"] = validation.minProperties.takeIf { it != NULL_STRING }
        extra["maxProperties"] = validation.maxProperties.takeIf { it != NULL_STRING }
    }

    getAnnotationsByType(Custom::class.java).forEach { custom ->
        extra[custom.name] = custom.value
    }

    context.env.elementUtils.getAllAnnotationMirrors(this@findExtra)
        .filterNot { it.annotationType.getFullName() == Metadata::class.qualifiedName }
        .onEach { annotation -> inDebug { it.info("TypeSchemaGenerator#findExtra | Annotation: ${annotation.annotationType}") } }
        .filter { annotation ->
            val isCustom = annotation.annotationType?.asElement()?.getAnnotation(CustomAnnotation::class.java) != null

            if (!isCustom) {
                inDebug {
                    it.info("TypeSchemaGenerator#findExtra | Usage: $annotation")
                    it.info("TypeSchemaGenerator#findExtra | Implementation:")
                    context.env.elementUtils.printElements(
                        MessagerWriter(context),
                        annotation.annotationType.asElement()
                    )
                }
            }

            isCustom
        }
        .flatMap { customAnnotation ->
            inDebug { it.info("TypeSchemaGenerator#findExtra | Custom annotation: $customAnnotation") }
            val elements = context.env.elementUtils.getElementValuesWithDefaults(customAnnotation)
            inDebug { it.info("TypeSchemaGenerator#findExtra | Element values with defaults: $elements") }
            elements.asSequence()
        }
        .forEach { (element, value) ->
            extra[element.toSimpleName()] = value.accept(object : AnnotationValueVisitor<Any, Nothing> {
                override fun visit(av: AnnotationValue, p: Nothing?) = av.value.toString()
                override fun visitBoolean(boolean: Boolean, p: Nothing?) = boolean
                override fun visitByte(byte: Byte, p: Nothing?) = byte
                override fun visitChar(char: Char, p: Nothing?) = char
                override fun visitDouble(double: Double, p: Nothing?) = double
                override fun visitFloat(float: Float, p: Nothing?) = float
                override fun visitInt(int: Int, p: Nothing?) = int
                override fun visitLong(long: Long, p: Nothing?) = long
                override fun visitShort(short: Short, p: Nothing?) = short
                override fun visitString(string: String, p: Nothing?) = string.trimIndent()
                override fun visitType(type: TypeMirror, p: Nothing?) = type.getFullName()
                override fun visitEnumConstant(variable: VariableElement, p: Nothing?) = variable.toSimpleName()
                override fun visitArray(values: MutableList<out AnnotationValue>, p: Nothing?): JsonArray = JsonArray().also { array ->
                    values.forEach {
                        when (val result = it.accept(this, null)) {
                            is Boolean -> array.add(result)
                            is Number -> array.add(result)
                            is String -> array.add(result)
                            is JsonElement -> array.add(result)
                            else -> throw UnsupportedOperationException("[CustomAnnotation] Unsupported array value: $it")
                        }
                    }
                }
                override fun visitAnnotation(annotationMirror: AnnotationMirror?, p: Nothing?) = throw UnsupportedOperationException("[CustomAnnotation] Unsupported nested annotations")
                override fun visitUnknown(av: AnnotationValue?, p: Nothing?) = throw UnsupportedOperationException("[CustomAnnotation] Unknown value $av")
            }, null)
            inDebug { it.info("TypeSchemaGenerator#findExtra | Visited entry ($element, $value) mapped to ${extra[element.toSimpleName()]}") }
        }

    extra
}

private fun JsonObject.addExtra(extra: Map<String, Any?>): JsonObject = also {
    extra
        .filterValues { it != null }
        .forEach { (key, value) ->
            when (value) {
                is JsonElement -> add(key, value)
                is Boolean -> addProperty(key, value)
                is Number -> addProperty(key, value)
                else -> addProperty(key, value.toString())
            }
        }
}