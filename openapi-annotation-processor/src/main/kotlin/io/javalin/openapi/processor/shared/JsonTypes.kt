package io.javalin.openapi.processor.shared

import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.StructureType
import io.javalin.openapi.experimental.StructureType.ARRAY
import io.javalin.openapi.experimental.StructureType.DEFAULT
import io.javalin.openapi.experimental.StructureType.DICTIONARY
import io.javalin.openapi.processor.OpenApiAnnotationProcessor.Companion.context
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

    class ClassDefinitionImpl(
        override val mirror: TypeMirror,
        override val source: Element,
        override var generics: List<ClassDefinition> = emptyList(),
        override val type: StructureType = DEFAULT
    ) : ClassDefinition {
        override val simpleName: String = mirror.getSimpleName()
        override val fullName: String = mirror.getFullName()

        override fun equals(other: Any?): Boolean =
            when {
                this === other -> true
                other is ClassDefinition -> this.fullName == other.fullName
                else -> false
            }

        override fun hashCode(): Int = fullName.hashCode()

    }

    private fun Element.toModel(generics: List<ClassDefinition> = emptyList(), type: StructureType = DEFAULT): ClassDefinition =
        ClassDefinitionImpl(asType(), this, generics, type)

    private val objectType by lazy { context.forTypeElement(Object::class.java.name)!!.asType() }
    private val collectionType by lazy { context.forTypeElement(Collection::class.java.name)!! }
    private val mapType by lazy { context.forTypeElement(Map::class.java.name)!! }

    fun TypeMirror.toClassDefinition(generics: List<ClassDefinition> = emptyList(), type: StructureType = DEFAULT): ClassDefinition {
        val types = context.env.typeUtils

        return when (this) {
            is TypeVariable -> upperBound?.toClassDefinition(generics, type) ?: lowerBound?.toClassDefinition(generics, type)
            is PrimitiveType -> types.boxedClass(this).toModel(generics, type)
            is ArrayType -> componentType.toClassDefinition(generics, type = ARRAY)
            is DeclaredType -> when {
                types.isAssignable(types.erasure(this), mapType.asType()) ->
                    ClassDefinitionImpl(
                        mirror = this,
                        source = mapType,
                        generics = listOfNotNull(
                            typeArguments.getOrElse(0) { objectType }.toClassDefinition(),
                            typeArguments.getOrElse(1) { objectType }.toClassDefinition()
                        ),
                        type = DICTIONARY
                    )
                types.isAssignable(types.erasure(this), collectionType.asType()) ->
                    typeArguments.getOrElse(0) { objectType }.toClassDefinition(generics, ARRAY)
                else ->
                    ClassDefinitionImpl(
                        mirror = this,
                        source = asElement(),
                        generics = typeArguments.mapNotNull { it.toClassDefinition() },
                        type = type
                    )
            }
            else -> types.asElement(this)?.toModel(generics, type)
        } ?: objectType.toClassDefinition()
    }

    fun detectContentType(typeMirror: TypeMirror): String {
        val model = typeMirror.toClassDefinition()

        return when {
            (model.type == ARRAY && model.simpleName == "Byte") || model.simpleName == "[B" || model.simpleName == "File" -> "application/octet-stream"
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