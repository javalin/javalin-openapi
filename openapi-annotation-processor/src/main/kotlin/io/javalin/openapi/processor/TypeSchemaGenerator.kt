package io.javalin.openapi.processor

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.javalin.openapi.AllOf
import io.javalin.openapi.AnyOf
import io.javalin.openapi.Combinator
import io.javalin.openapi.Custom
import io.javalin.openapi.OneOf
import io.javalin.openapi.OpenApiByFields
import io.javalin.openapi.OpenApiExample
import io.javalin.openapi.OpenApiIgnore
import io.javalin.openapi.OpenApiName
import io.javalin.openapi.OpenApiPropertyType
import io.javalin.openapi.Visibility
import io.javalin.openapi.processor.shared.JsonTypes
import io.javalin.openapi.processor.shared.JsonTypes.DataModel
import io.javalin.openapi.processor.shared.JsonTypes.DataType.ARRAY
import io.javalin.openapi.processor.shared.JsonTypes.DataType.DICTIONARY
import io.javalin.openapi.processor.shared.JsonTypes.getTypeMirror
import io.javalin.openapi.processor.shared.JsonTypes.getTypeMirrors
import io.javalin.openapi.processor.shared.JsonTypes.toModel
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

internal fun createTypeSchema(type: DataModel, inlineRefs: Boolean): ResultScheme {
    val schema = JsonObject()
    val propertiesObject = JsonObject()
    val properties = type.findAllProperties()
    val references = ArrayList<TypeMirror>()

    properties.forEach { property ->
        val (propertySchema, refs) = createTypeDescription(property.type.toModel()!!, inlineRefs, property.combinator, property.extra)
        propertiesObject.add(property.name, propertySchema)
        references.addAll(refs)
    }

    schema.addProperty("type", "object")
    schema.add("properties", propertiesObject)

    if (properties.any { it.required }) {
        val required = JsonArray()
        properties.filter { it.required}.forEach { required.add(it.name) }
        schema.add("required", required)
    }

    return ResultScheme(schema, references)
}

internal fun createTypeDescription(
    model: DataModel,
    inlineRefs: Boolean = false,
    propertyCombinator: PropertyCombinator? = null,
    extra: Map<String, Any?> = emptyMap()
): ResultScheme {
    val scheme = JsonObject()
    val references = mutableListOf<TypeMirror>()

    when {
        propertyCombinator != null -> {
            val combinatorObject = JsonArray()
            propertyCombinator.second.forEach { variantType ->
                val (variantScheme, refs) = createTypeSchema(variantType.toModel()!!, inlineRefs)
                combinatorObject.add(variantScheme)
                references.addAll(refs)
            }
            scheme.add(propertyCombinator.first.propertyName, combinatorObject)
        }
        model.type == ARRAY && model.simpleName == "Byte" -> {
            scheme.addProperty("type", "string")
            scheme.addProperty("format", "binary")
        }
        model.type == ARRAY -> {
            scheme.addProperty("type", "array")
            val items = JsonObject()
            items.addType(model, inlineRefs, references)
            scheme.add("items", items)
        }
        model.type == DICTIONARY -> {
            scheme.addProperty("type", "object")
            val additionalProperties = JsonObject()
            additionalProperties.addType(model.generics[1], inlineRefs, references)
            scheme.add("additionalProperties", additionalProperties)
        }
        model.sourceElement.kind == ENUM -> {
            val values = JsonArray()
            model.sourceElement.enclosedElements
                .filterIsInstance<VariableElement>()
                .map { it.simpleName.toString() }
                .forEach { values.add(it) }
            scheme.addProperty("type", "string")
            scheme.add("enum", values)
        }
        else -> scheme.addType(model, inlineRefs, references)
    }

    extra
        .filterValues { it != null }
        .forEach { (key, value) ->
            when (value) {
                is Boolean -> scheme.addProperty(key, value)
                is Number -> scheme.addProperty(key, value)
                else -> scheme.addProperty(key, value.toString())
            }
        }

    return ResultScheme(scheme, references)
}

internal fun JsonObject.addType(model: DataModel, inlineRefs: Boolean, references: MutableCollection<TypeMirror>) {
    val nonRefType = JsonTypes.NON_REF_TYPES[model.simpleName]

    if (nonRefType == null) {
        if (inlineRefs) {
            val (subScheme, subReferences) = createTypeSchema(model, inlineRefs)
            subScheme.asMap().forEach { (key, value) -> add(key, value) }
            references.addAll(subReferences)
        } else {
            references.add(model.typeMirror)
            addProperty("\$ref", "#/components/schemas/${model.simpleName}")
        }
        return
    }

    addProperty("type", nonRefType.type)

    nonRefType.format
        .takeIf { it.isNotEmpty() }
        ?.also { addProperty("format", it) }
}

typealias PropertyCombinator = Pair<Combinator, Collection<TypeMirror>>

data class Property(
    val name: String,
    val type: TypeMirror,
    val combinator: PropertyCombinator?,
    val required: Boolean,
    val extra: MutableMap<String, Any?> = mutableMapOf()
)

private val objectType by lazy { OpenApiAnnotationProcessor.elements.getTypeElement("java.lang.Object") }
private val recordType by lazy { OpenApiAnnotationProcessor.elements.getTypeElement("java.lang.Record") }

internal fun DataModel.findAllProperties(): Collection<Property> {
    val acceptFields = sourceElement.getAnnotation(OpenApiByFields::class.java)

    val isRecord = when (recordType) {
        null -> false
        else -> OpenApiAnnotationProcessor.types.isAssignable(typeMirror, recordType.asType())
    }

    val properties = mutableListOf<Property>()

    for (property in sourceElement.enclosedElements) {
        if (property is Element) {
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

                if (acceptFields.value.priority > fieldVisibility.priority) {
                    continue
                }
            }

            if (property.getAnnotation(OpenApiIgnore::class.java) != null) {
                continue
            }

            if (objectType.enclosedElements.any { it.simpleName == property.simpleName }) {
                continue
            }

            val simpleName = property.simpleName.toString()
            val customName = property.getAnnotation(OpenApiName::class.java)

            val name = when {
                customName != null -> customName.value
                isRecord || acceptFields != null -> simpleName
                simpleName.startsWith("get") -> simpleName.replaceFirst("get", "").replaceFirstChar { it.lowercase() }
                simpleName.startsWith("is") -> simpleName.replaceFirst("is", "").replaceFirstChar { it.lowercase() }
                else -> continue
            }

            val combinator = property.getAnnotation(OneOf::class.java)?.let { Combinator.ONE_OF to it.getTypeMirrors { value } }
                ?: property.getAnnotation(AnyOf::class.java)?.let { Combinator.ANY_OF to it.getTypeMirrors { value } }
                ?: property.getAnnotation(AllOf::class.java)?.let { Combinator.ALL_OF to it.getTypeMirrors { value } }

            val propertyType = property.getAnnotation(OpenApiPropertyType::class.java)
                ?.getTypeMirror { definedBy }
                ?: (property as? ExecutableElement)?.returnType
                ?: (property as? VariableElement)?.asType()
                ?: continue

            val extra = mutableMapOf<String, Any?>(
                "example" to property.getAnnotation(OpenApiExample::class.java)?.value
            )

            property.getAnnotationsByType(Custom::class.java).forEach { custom ->
                extra[custom.name] = custom.value
            }

            properties.add(
                Property(
                    name = name,
                    type = propertyType,
                    combinator = combinator,
                    required = propertyType.kind.isPrimitive || property.annotationMirrors.any { it.annotationType.asElement().simpleName.contentEquals("NotNull") },
                    extra = extra
                )
            )
        }
    }

    return properties
}