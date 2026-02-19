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
        with(mirror) {
            when (this) {
                is TypeVariable ->
                    upperBound?.toClassDefinition(generics, type) ?: lowerBound?.toClassDefinition(generics, type)
                is ArrayType ->
                    componentType.toClassDefinition(generics, type = ARRAY)
                is PrimitiveType -> {
                    val boxedMirror = types.boxedClass(this).asType()
                    val boxedElement = types.boxedClass(this)
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
                        types.isAssignable(types.erasure(this), mapType().asType()) ->
                            ClassDefinition(
                                simpleName = context.inContext { this@with.getSimpleName() },
                                fullName = context.inContext { this@with.getFullName() },
                                generics = listOfNotNull(
                                    typeArguments.getOrElse(0) { objectType().asType() }.toClassDefinition(),
                                    typeArguments.getOrElse(1) { objectType().asType() }.toClassDefinition()
                                ),
                                structureType = DICTIONARY,
                                handle = ClassDefinitionHandle(this, mapType())
                            )
                        types.isAssignable(types.erasure(this), collectionType().asType()) ->
                            typeArguments.getOrElse(0) { objectType().asType() }.toClassDefinition(generics, ARRAY)
                        else ->
                            ClassDefinition(
                                simpleName = context.inContext { this@with.getSimpleName() },
                                fullName = context.inContext { this@with.getFullName() },
                                generics = typeArguments.mapNotNull { it.toClassDefinition() },
                                structureType = type,
                                handle = ClassDefinitionHandle(this, asElement())
                            )
                    }
                else ->
                    types.asElement(this)?.asType()?.toClassDefinition(generics, type)
            }
        } ?: objectType().asType().toClassDefinition(type = type)
    }
