package io.javalin.openapi.processor.shared

import io.javalin.openapi.processor.OpenApiAnnotationProcessor
import io.javalin.openapi.processor.shared.JsonTypes.DataType.ARRAY
import io.javalin.openapi.processor.shared.JsonTypes.DataType.DEFAULT
import io.javalin.openapi.processor.shared.JsonTypes.DataType.DICTIONARY
import javax.lang.model.element.Element
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable
import kotlin.reflect.KClass

internal object JsonTypes {

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
        "Long" to Data("integer", "int64"),

        "Float" to Data("number", "float"),
        "Double" to Data("number", "double"),

        "Char" to Data("string"),
        "Character" to Data("string"),
        "String" to Data("string"),
        "BigDecimal" to Data("string"),
        "UUID" to Data("string"),
        "ObjectId" to Data("string"),

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

    private val objectType by lazy { OpenApiAnnotationProcessor.elements.getTypeElement(Object::class.java.name).asType() }
    private val collectionType by lazy { OpenApiAnnotationProcessor.elements.getTypeElement(Collection::class.java.name) }
    private val mapType by lazy { OpenApiAnnotationProcessor.elements.getTypeElement(Map::class.java.name) }

    fun TypeMirror.toModel(generics: List<DataModel> = emptyList(), type: DataType = DEFAULT): DataModel {
        val types = OpenApiAnnotationProcessor.types

        return when (this) {
            is TypeVariable -> upperBound?.toModel(generics, type) ?: lowerBound?.toModel(generics, type)
            is PrimitiveType -> types.boxedClass(this).toModel(generics, type)
            is ArrayType -> componentType.toModel(generics, type = ARRAY)
            is DeclaredType -> when {
                types.isAssignable(types.erasure(this), mapType.asType()) ->
                    DataModel(
                        typeMirror = this,
                        sourceElement = mapType,
                        generics = listOfNotNull(
                            typeArguments.getOrElse(0) { objectType }.toModel(),
                            typeArguments.getOrElse(1) { objectType }.toModel()
                        ),
                        type = DICTIONARY
                    )
                    types.isAssignable(types.erasure(this), collectionType.asType()) ->
                        typeArguments.getOrElse(0) { objectType }.toModel(generics, ARRAY)
                else ->
                    DataModel(
                        typeMirror = this,
                        sourceElement = asElement(),
                        generics = typeArguments.mapNotNull { it.toModel() },
                        type = type
                    )
            }
            else -> types.asElement(this)?.toModel(generics, type)
        } ?: objectType.toModel()
    }

    fun detectContentType(typeMirror: TypeMirror): String {
        val model = typeMirror.toModel()

        return when {
            (model.type == ARRAY && model.simpleName == "Byte") || model.simpleName == "[B" -> "application/octet-stream"
            model.type == ARRAY -> "application/json"
            model.simpleName == "String" -> "text/plain"
            else -> "application/json"
        }
    }

    fun <A : Annotation> A.getTypeMirrors(supplier: A.() -> Array<out KClass<*>>): List<TypeMirror> =
        try {
            throw Error(supplier().toString()) // always throws MirroredTypesException, because we cannot get Class instance from annotation at compile-time
        } catch (mirroredTypeException: MirroredTypesException) {
            mirroredTypeException.typeMirrors
        }

    fun <A : Annotation> A.getTypeMirror(supplier: A.() -> KClass<*>): TypeMirror =
        try {
            throw Error(supplier().toString()) // always throws MirroredTypeException, because we cannot get Class instance from annotation at compile-time
        } catch (mirroredTypeException: MirroredTypeException) {
            mirroredTypeException.typeMirror
        }

}