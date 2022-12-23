package io.javalin.openapi.processor.shared

import io.javalin.openapi.OpenApiName
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.Extra
import io.javalin.openapi.experimental.StructureType
import io.javalin.openapi.experimental.StructureType.ARRAY
import io.javalin.openapi.experimental.StructureType.DEFAULT
import io.javalin.openapi.experimental.StructureType.DICTIONARY
import io.javalin.openapi.processor.OpenApiAnnotationProcessor.Companion.context
import javax.lang.model.element.Element
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable
import kotlin.reflect.KClass

private val objectType by lazy { context.forTypeElement(Object::class.java.name)!!.asType() }
private val collectionType by lazy { context.forTypeElement(Collection::class.java.name)!! }
private val mapType by lazy { context.forTypeElement(Map::class.java.name)!! }

class ClassDefinitionImpl(
    override val mirror: TypeMirror,
    override val source: Element,
    override var generics: List<ClassDefinition> = emptyList(),
    override val type: StructureType = DEFAULT,
    override val extra: MutableList<Extra> = mutableListOf()
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

fun TypeMirror.toClassDefinition(generics: List<ClassDefinition> = emptyList(), type: StructureType = DEFAULT): ClassDefinition {
    val types = context.env.typeUtils

    return when (this) {
        is TypeVariable -> upperBound?.toClassDefinition(generics, type) ?: lowerBound?.toClassDefinition(generics, type)
        is PrimitiveType -> types.boxedClass(this).toClassDefinition(generics, type)
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
        else -> types.asElement(this)?.toClassDefinition(generics, type)
    } ?: objectType.toClassDefinition()
}

private fun Element.toClassDefinition(generics: List<ClassDefinition> = emptyList(), type: StructureType = DEFAULT): ClassDefinition =
    ClassDefinitionImpl(asType(), this, generics, type)

fun TypeMirror.isPrimitive(): Boolean =
    kind.isPrimitive

fun TypeMirror.getSimpleName(): String =
    getFullName().substringAfterLast(".")

fun TypeMirror.getFullName(): String =
    context.env.typeUtils.asElement(this)
        ?.getAnnotation(OpenApiName::class.java)
        ?.value
        ?.let { toString().substringBeforeLast(".") + "." + it }
        ?: toString().substringBefore("<")

fun Element.hasAnnotation(simpleName: String): Boolean =
    annotationMirrors.any { it.annotationType.asElement().simpleName.contentEquals(simpleName) }

fun Element.getFullName(): String =
    toString()

fun Element.toSimpleName(): String =
    simpleName.toString()

fun VariableElement.toSimpleName(): String =
    simpleName.toString()

fun <A : Annotation> A.getClassDefinitions(supplier: A.() -> Array<out KClass<*>>): Set<ClassDefinition> =
    getTypeMirrors(supplier)
        .map { it.toClassDefinition() }
        .toSet()

fun <A : Annotation> A.getTypeMirrors(supplier: A.() -> Array<out KClass<*>>): Set<TypeMirror> =
    try {
        throw Error(supplier().toString()) // always throws MirroredTypesException, because we cannot get Class instance from annotation at compile-time
    } catch (mirroredTypeException: MirroredTypesException) {
        mirroredTypeException.typeMirrors.toSet()
    }

fun <A : Annotation> A.getClassDefinition(supplier: A.() -> KClass<*>): ClassDefinition =
    getTypeMirror(supplier).toClassDefinition()

fun <A : Annotation> A.getTypeMirror(supplier: A.() -> KClass<*>): TypeMirror =
    try {
        throw Error(supplier().toString()) // always throws MirroredTypeException, because we cannot get Class instance from annotation at compile-time
    } catch (mirroredTypeException: MirroredTypeException) {
        mirroredTypeException.typeMirror
    }