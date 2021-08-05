package io.javalin.openapi.processor.utils

import io.javalin.openapi.processor.OpenApiAnnotationProcessor
import javax.lang.model.element.Element
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeMirror


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
        "Instant" to Data("string", "date-time")
    )

    data class Type(
        val element: Element,
        val dimensions: Int
    ) {

        fun isArray(): Boolean =
            dimensions > 0

        fun getSimpleName(): String =
            element.simpleName.toString()

    }

    fun getType(typeMirror: TypeMirror): Type {
        var dimensions = 0
        var type = typeMirror

        while (type is ArrayType) {
            type = type.componentType
            dimensions++
        }

        val types = OpenApiAnnotationProcessor.types
        val collectionType = OpenApiAnnotationProcessor.elements.getTypeElement(Collection::class.java.name)

        while (types.isAssignable(types.erasure(type), collectionType.asType())) {
            type = (type as DeclaredType).typeArguments[0]
            dimensions++
        }

        val element =
            if (type is PrimitiveType) {
                types.boxedClass(type)
            }
            else {
                types.asElement(type)
            }

        return Type(element, dimensions)
    }

    fun detectContentType(typeMirror: TypeMirror): String {
        val type = getType(typeMirror)

        if (type.isArray()) {
            return "application/json"
        }

        return when (type.getSimpleName()) {
            "String" -> "text/plain"
            "ByteArray", "[B" -> "application/octet-stream"
            else -> "application/json"
        }
    }

}