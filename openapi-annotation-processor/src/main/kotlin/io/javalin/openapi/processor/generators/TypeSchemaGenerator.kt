package io.javalin.openapi.processor.generators

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.javalin.openapi.Custom
import io.javalin.openapi.CustomAnnotation
import io.javalin.openapi.JsonSchema
import io.javalin.openapi.OpenApiByFields
import io.javalin.openapi.OpenApiExample
import io.javalin.openapi.OpenApiIgnore
import io.javalin.openapi.OpenApiName
import io.javalin.openapi.OpenApiPropertyType
import io.javalin.openapi.Visibility
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.StructureType.ARRAY
import io.javalin.openapi.experimental.StructureType.DICTIONARY
import io.javalin.openapi.processor.OpenApiAnnotationProcessor
import io.javalin.openapi.processor.OpenApiAnnotationProcessor.Companion.context
import io.javalin.openapi.processor.shared.JsonTypes.getTypeMirror
import io.javalin.openapi.processor.shared.JsonTypes.toClassDefinition
import io.javalin.openapi.processor.shared.MessagerWriter
import io.javalin.openapi.processor.shared.getFullName
import io.javalin.openapi.processor.shared.hasAnnotation
import io.javalin.openapi.processor.shared.inDebug
import io.javalin.openapi.processor.shared.info
import io.javalin.openapi.processor.shared.isPrimitive
import io.javalin.openapi.processor.shared.toSimpleName
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.AnnotationValueVisitor
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.ENUM
import javax.lang.model.element.ElementKind.METHOD
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

data class ResultScheme(
    val json: JsonObject,
    val references: Collection<TypeMirror>
)

internal fun createTypeSchema(
    type: ClassDefinition,
    inlineRefs: Boolean = false,
    requireNonNullsByDefault: Boolean = true
): ResultScheme {
    val typeElement = type.source
    val definedBy = typeElement.getAnnotation(OpenApiPropertyType::class.java)?.getTypeMirror { definedBy }

    if (definedBy != null) {
        return createTypeSchema(definedBy.toClassDefinition(), inlineRefs, requireNonNullsByDefault)
    }

    val schema = JsonObject()
    val references = mutableListOf<TypeMirror>()
    val composition = typeElement.getComposition()

    when {
        composition != null -> {
            schema.createComposition(type, composition, references, inlineRefs, requireNonNullsByDefault)
        }
        typeElement.kind == ENUM -> {
            val values = JsonArray()
            typeElement.enclosedElements
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

            val extra = typeElement.findExtra()
            schema.addExtra(extra)

            val propertiesObject = JsonObject()
            schema.add("properties", propertiesObject)

            val requireNonNulls = typeElement.getAnnotation(JsonSchema::class.java)
                ?.requireNonNulls
                ?: requireNonNullsByDefault

            val properties = type.findAllProperties(requireNonNulls)

            properties.forEach { property ->
                val (propertySchema, refs) = createTypeDescription(property.type.toClassDefinition(), inlineRefs, requireNonNulls, property.composition, property.extra)
                propertiesObject.add(property.name, propertySchema)
                references.addAll(refs)
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


internal fun createTypeDescription(
    type: ClassDefinition,
    inlineRefs: Boolean = false,
    requiresNonNulls: Boolean = true,
    propertyComposition: PropertyComposition? = null,
    extra: Map<String, Any?> = emptyMap()
): ResultScheme {
    val definedBy = type.source.getAnnotation(OpenApiPropertyType::class.java)?.getTypeMirror { definedBy }

    if (definedBy != null) {
        return createTypeDescription(definedBy.toClassDefinition(), inlineRefs, requiresNonNulls, propertyComposition, extra)
    }

    val scheme = JsonObject()
    val references = mutableListOf<TypeMirror>()

    when {
        propertyComposition != null -> {
            scheme.createComposition(type, propertyComposition, references, inlineRefs, requiresNonNulls)
        }
        type.type == ARRAY && type.simpleName == "Byte" -> {
            scheme.addProperty("type", "string")
            scheme.addProperty("format", "binary")
        }
        type.type == ARRAY -> {
            scheme.addProperty("type", "array")
            val items = JsonObject()
            items.addType(type, inlineRefs, references, requiresNonNulls)
            scheme.add("items", items)
        }
        type.type == DICTIONARY -> {
            scheme.addProperty("type", "object")
            val additionalProperties = JsonObject()
            additionalProperties.addType(type.generics[1], inlineRefs, references, requiresNonNulls)
            scheme.add("additionalProperties", additionalProperties)
        }
        else -> scheme.addType(type, inlineRefs, references, requiresNonNulls)
    }

    scheme.addExtra(extra)
    return ResultScheme(scheme, references)
}

internal fun JsonObject.addType(model: ClassDefinition, inlineRefs: Boolean, references: MutableCollection<TypeMirror>, requiresNonNulls: Boolean) {
    val nonRefType = OpenApiAnnotationProcessor.configuration.simpleTypeMappings[model.fullName]

    if (nonRefType == null) {
        if (inlineRefs) {
            val (subScheme, subReferences) = createTypeSchema(model, true, requiresNonNulls)
            subScheme.asMap().forEach { (key, value) -> add(key, value) }
            references.addAll(subReferences)
        } else {
            references.add(model.mirror)
            addProperty("\$ref", "#/components/schemas/${model.simpleName}")
        }
        return
    }

    addProperty("type", nonRefType.type)

    nonRefType.format
        .takeIf { it.isNotEmpty() }
        ?.also { addProperty("format", it) }
}

data class Property(
    val name: String,
    val type: TypeMirror,
    val composition: PropertyComposition?,
    val required: Boolean,
    val extra: Map<String, Any?>
)

private val objectType by lazy { context.forTypeElement("java.lang.Object")!! }
private val recordType by lazy { context.forTypeElement("java.lang.Record") }

internal fun ClassDefinition.findAllProperties(requireNonNulls: Boolean): Collection<Property> {
    val acceptFields = source.getAnnotation(OpenApiByFields::class.java)
        ?.value

    val isRecord = when (recordType) {
        null -> false
        else -> context.isAssignable(mirror, recordType!!.asType())
    }

    inDebug { it.info("TypeSchemaGenerator#findAllProperties | Enclosed elements of ${this.mirror}: ${source.enclosedElements}") }
    val properties = mutableListOf<Property>()

    for (property in source.enclosedElements) {
        if (property is Element) {
            if (OpenApiAnnotationProcessor.configuration.propertyInSchemeFilter?.filter(context, this, property) == false) {
                continue
            }

            if (property.modifiers.contains(Modifier.STATIC)) {
                continue
            }

            if (property.kind != METHOD && acceptFields == null) {
                continue
            }

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

            if (property.getAnnotation(OpenApiIgnore::class.java) != null) {
                continue
            }

            if (objectType.enclosedElements.any { it.toSimpleName() == property.toSimpleName() }) {
                continue
            }

            val simpleName = property.toSimpleName()
            val customName = property.getAnnotation(OpenApiName::class.java)

            val name = when {
                customName != null -> customName.value
                isRecord || acceptFields != null -> simpleName
                simpleName.startsWith("get") -> simpleName.replaceFirst("get", "").replaceFirstChar { it.lowercase() }
                simpleName.startsWith("is") -> simpleName.replaceFirst("is", "").replaceFirstChar { it.lowercase() }
                else -> continue
            }

            val propertyType = property.getAnnotation(OpenApiPropertyType::class.java)
                ?.getTypeMirror { definedBy }
                ?: (property as? ExecutableElement)?.returnType
                ?: (property as? VariableElement)?.asType()
                ?: continue

            properties.add(
                Property(
                    name = name,
                    type = propertyType,
                    composition = property.getComposition(),
                    required = requireNonNulls && (propertyType.isPrimitive() || property.hasAnnotation("NotNull")),
                    extra = property.findExtra()
                )
            )
        }
    }

    return properties
}

private fun Element.findExtra(): Map<String, Any?> {
    val extra = mutableMapOf<String, Any?>(
        "example" to getAnnotation(OpenApiExample::class.java)?.value
    )

    getAnnotationsByType(Custom::class.java).forEach { custom ->
        extra[custom.name] = custom.value
    }

    context.env.elementUtils.getAllAnnotationMirrors(this)
        .filterNot { it.annotationType.getFullName() == Metadata::class.qualifiedName }
        .onEach { annotation -> inDebug { it.info("TypeSchemaGenerator#findExtra | Annotation: ${annotation.annotationType}") } }
        .filter { annotation ->
            val isCustom = annotation.annotationType.asElement().getAnnotation(CustomAnnotation::class.java) != null

            if (!isCustom) {
                inDebug {
                    it.info("TypeSchemaGenerator#findExtra | Usage: $annotation")
                    it.info("TypeSchemaGenerator#findExtra | Implementation:")
                    context.env.elementUtils.printElements(
                        MessagerWriter(),
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

    return extra
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