package net.dzikoysk.openapi.processor.utils

import net.dzikoysk.openapi.processor.OpenApiAnnotationProcessor
import javax.lang.model.type.TypeMirror

object TypesUtils {

    data class Data(
        val type: String,
        val format: String = ""
    ) {

        val hasFormat = format.isEmpty()

    }

    private val NON_REF_TYPES: Map<String, Data> = mapOf(
        "Boolean" to Data("boolean"),

        "Int" to Data("integer", "int32"),
        "Integer" to Data("integer", "int32"),

        "Float" to Data("number", "float"),
        "Double" to Data("number", "double"),

        "String" to Data("string"),
        "ByteArray" to Data("string", "binary"),

        "List" to Data(""),
        "Long" to Data(""),
        "BigDecimal" to Data(""),
        "Date" to Data(""),
        "LocalDate" to Data(""),
        "LocalDateTime" to Data(""),
        "Instant" to Data("")
    )

    fun detectContentType(type: Class<*>): String =
        when {
            String::class.java.isAssignableFrom(type) -> "text/plain"
            ByteArray::class.java.isAssignableFrom(type) -> "application/octet-stream"
            else -> "application/json"
        }

    fun getType(typeMirror: TypeMirror?) {
        val element = OpenApiAnnotationProcessor.types.asElement(typeMirror)
        val enclosedElements = element.enclosedElements
    }

}