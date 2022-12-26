package io.javalin.openapi.experimental

import io.javalin.openapi.experimental.StructureType.ARRAY
import io.javalin.openapi.experimental.StructureType.DEFAULT
import io.javalin.openapi.experimental.StructureType.DICTIONARY
import io.javalin.openapi.experimental.processor.shared.collectionType
import io.javalin.openapi.experimental.processor.shared.mapType
import io.javalin.openapi.experimental.processor.shared.objectType
import javax.lang.model.element.Element
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable

class ClassDefinition(
    val context: AnnotationProcessorContext,
    val mirror: TypeMirror,
    val source: Element,
    val generics: List<ClassDefinition> = emptyList(),
    val structureType: StructureType = DEFAULT,
    val extra: MutableList<Extra> = mutableListOf()
) {

    val simpleName: String = context.inContext { mirror.getSimpleName() }
    val fullName: String = context.inContext { mirror.getFullName() }

    companion object {

        @JvmStatic
        fun classDefinitionFrom(
            context: AnnotationProcessorContext,
            mirror: TypeMirror,
            generics: List<ClassDefinition> = emptyList(),
            type: StructureType = DEFAULT
        ): ClassDefinition =
            with(context) {
                with (mirror) {
                    when (this) {
                        is TypeVariable -> upperBound?.toClassDefinition(generics, type) ?: lowerBound?.toClassDefinition(generics, type)
                        is ArrayType -> componentType.toClassDefinition(generics, type = ARRAY)
                        is PrimitiveType ->
                            ClassDefinition(
                                context = context,
                                mirror = types.boxedClass(this).asType(),
                                source = types.boxedClass(this),
                                generics = generics,
                                structureType = type
                            )
                        is DeclaredType -> when {
                            types.isAssignable(types.erasure(this), mapType().asType()) ->
                                ClassDefinition(
                                    context = context,
                                    mirror = this,
                                    source = mapType(),
                                    generics = listOfNotNull(
                                        typeArguments.getOrElse(0) { objectType().asType() }.toClassDefinition(),
                                        typeArguments.getOrElse(1) { objectType().asType() }.toClassDefinition()
                                    ),
                                    structureType = DICTIONARY
                                )
                            types.isAssignable(types.erasure(this), collectionType().asType()) ->
                                typeArguments.getOrElse(0) { objectType().asType() }.toClassDefinition(generics, ARRAY)
                            else ->
                                ClassDefinition(
                                    context = context,
                                    mirror = this,
                                    source = asElement(),
                                    generics = typeArguments.mapNotNull { it.toClassDefinition() },
                                    structureType = type
                                )
                        }
                        else -> types.asElement(this)?.asType()?.toClassDefinition(generics, type)
                    }
                } ?: objectType().asType().toClassDefinition()
            }

    }

    override fun equals(other: Any?): Boolean =
        when {
            this === other -> true
            other is ClassDefinition -> this.fullName == other.fullName
            else -> false
        }

    override fun hashCode(): Int = fullName.hashCode()

}

enum class StructureType {
    DEFAULT,
    ARRAY,
    DICTIONARY
}

interface Extra

class CustomProperty(
    val name: String,
    val type: ClassDefinition
) : Extra
