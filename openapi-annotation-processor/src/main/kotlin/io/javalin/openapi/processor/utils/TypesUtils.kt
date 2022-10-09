package io.javalin.openapi.processor.utils

import io.javalin.openapi.processor.OpenApiAnnotationProcessor
import io.javalin.openapi.processor.utils.TypesUtils.DataType.ARRAY
import io.javalin.openapi.processor.utils.TypesUtils.DataType.DEFAULT
import io.javalin.openapi.processor.utils.TypesUtils.DataType.DICTIONARY
import javax.lang.model.element.Element
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable

internal object TypesUtils {

    data class Data(
        val type: String,
        val format: String = ""
    )

    val NON_REF_TYPES: Map<String, Data> = mapOf(
        "Boolean" to Data("boolean"),

        "Byte" to Data("integer", "int32"),
        "Short" to Data("integer", "int32"),
        "Int" to Data("integer", "int32"),
        "Integer" to Data("integer", "int32"),
        "Long" to Data("number", "int64"),

        "Float" to Data("number", "float"),
        "Double" to Data("number", "double"),

        "Char" to Data("string"),
        "Character" to Data("string"),
        "String" to Data("string"),
        "BigDecimal" to Data("string"),
        "UUID" to Data("string"),

        "ByteArray" to Data("string", "binary"),
        "InputStream" to Data("string", "binary"),

        "Date" to Data("string", "date"),
        "LocalDate" to Data("string", "date"),

        "LocalDateTime" to Data("string", "date-time"),
        "Instant" to Data("string", "date-time"),

        "Object" to Data("object"),
        "Map" to Data("object"),
    )

    enum class DataType {
        DEFAULT,
        ARRAY,
        DICTIONARY
    }

    data class DataModel(
        val typeMirror: TypeMirror,
        val sourceElement: Element,
        var generics: List<DataModel> = emptyList(),
        val type: DataType = DEFAULT
    ) {
        val simpleName: String = sourceElement.simpleName.toString()
    }

    private fun Element.toModel(generics: List<DataModel> = emptyList(), type: DataType = DEFAULT): DataModel =
        DataModel(asType(), this, generics, type)

    fun TypeMirror.toModel(generics: List<DataModel> = emptyList(), type: DataType = DEFAULT): DataModel? {
        val types = OpenApiAnnotationProcessor.types
        val collectionType = OpenApiAnnotationProcessor.elements.getTypeElement(Collection::class.java.name)
        val mapType = OpenApiAnnotationProcessor.elements.getTypeElement(Map::class.java.name)

        return when (this) {
            is TypeVariable -> upperBound?.toModel(generics, type) ?: lowerBound?.toModel(generics, type)
            is PrimitiveType -> types.boxedClass(this).toModel(generics, type)
            is ArrayType -> componentType.toModel(generics, type = ARRAY)
            is DeclaredType -> when {
                types.isAssignable(types.erasure(this), mapType.asType()) -> DataModel(this, mapType, listOfNotNull(typeArguments[0]?.toModel(), typeArguments[1]?.toModel()), DICTIONARY)
                types.isAssignable(types.erasure(this), collectionType.asType()) -> typeArguments[0]?.toModel(generics, ARRAY)
                else -> DataModel(this, asElement(), typeArguments.mapNotNull { it.toModel() }, type)
            }
            else -> types.asElement(this)?.toModel(generics, type)
        }
    }

    fun detectContentType(typeMirror: TypeMirror): String {
        val model = typeMirror.toModel() ?: return ""

        return when {
            model.type == ARRAY -> "application/json"
            model.simpleName == "String" -> "text/plain"
            model.simpleName == "ByteArray" || model.simpleName == "[B" -> "application/octet-stream"
            else -> "application/json"
        }
    }

}