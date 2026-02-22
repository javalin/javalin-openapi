package io.javalin.openapi.experimental

import io.javalin.openapi.experimental.StructureType.ARRAY
import io.javalin.openapi.experimental.StructureType.DEFAULT
import io.javalin.openapi.experimental.StructureType.DICTIONARY
import io.javalin.openapi.experimental.processor.shared.collectionType
import io.javalin.openapi.experimental.processor.shared.mapType
import io.javalin.openapi.experimental.processor.shared.objectType
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable

fun classDefinitionFrom(
    context: AnnotationProcessorContext,
    mirror: TypeMirror,
    generics: List<ClassDefinition> = emptyList(),
    type: StructureType = DEFAULT
): ClassDefinition =
    with(context) {
        when (mirror) {
            is TypeVariable ->
                mirror.upperBound?.toClassDefinition(generics, type) ?: mirror.lowerBound?.toClassDefinition(generics, type)
            is ArrayType ->
                mirror.componentType.toClassDefinition(generics, type = ARRAY)
            is PrimitiveType -> {
                val boxedMirror = types.boxedClass(mirror).asType()
                val boxedElement = types.boxedClass(mirror)
                ClassDefinition(
                    simpleName = context.inContext { boxedMirror.getSimpleName() },
                    fullName = context.inContext { boxedMirror.getFullName() },
                    generics = generics,
                    structureType = type,
                    handle = ClassDefinitionHandle(boxedMirror, boxedElement)
                )
            }
            is DeclaredType ->
                when {
                    types.isAssignable(types.erasure(mirror), mapType().asType()) ->
                        ClassDefinition(
                            simpleName = context.inContext { mirror.getSimpleName() },
                            fullName = context.inContext { mirror.getFullName() },
                            generics = listOfNotNull(
                                mirror.typeArguments.getOrElse(0) { objectType().asType() }.toClassDefinition(),
                                mirror.typeArguments.getOrElse(1) { objectType().asType() }.toClassDefinition()
                            ),
                            structureType = DICTIONARY,
                            handle = ClassDefinitionHandle(mirror, mapType())
                        )
                    types.isAssignable(types.erasure(mirror), collectionType().asType()) ->
                        mirror.typeArguments.getOrElse(0) { objectType().asType() }.toClassDefinition(generics, ARRAY)
                    else ->
                        ClassDefinition(
                            simpleName = context.inContext { mirror.getSimpleName() },
                            fullName = context.inContext { mirror.getFullName() },
                            generics = mirror.typeArguments.mapNotNull { it.toClassDefinition() },
                            structureType = type,
                            handle = ClassDefinitionHandle(mirror, mirror.asElement())
                        )
                }
            else ->
                types.asElement(mirror)?.asType()?.toClassDefinition(generics, type)
        } ?: objectType().asType().toClassDefinition(type = type)
    }
