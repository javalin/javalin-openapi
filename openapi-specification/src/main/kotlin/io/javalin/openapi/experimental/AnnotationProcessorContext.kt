package io.javalin.openapi.experimental

import com.sun.source.util.Trees
import io.javalin.openapi.OpenApiName
import io.javalin.openapi.experimental.StructureType.DEFAULT
import io.javalin.openapi.experimental.processor.generators.TypeSchemaGenerator
import io.javalin.openapi.experimental.processor.shared.getTypeMirror
import io.javalin.openapi.experimental.processor.shared.getTypeMirrors
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types
import kotlin.reflect.KClass

class AnnotationProcessorContext(
    val configuration: OpenApiAnnotationProcessorConfiguration,
    val env: ProcessingEnvironment,
    val trees: Trees?,
) {

    val types: Types = env.typeUtils
    val typeSchemaGenerator: TypeSchemaGenerator = TypeSchemaGenerator(this)
    var roundEnv: RoundEnvironment? = null

    fun <R> inContext(body: AnnotationProcessorContext.() -> R): R =
        body()

    fun inDebug(body: (Messager) -> Unit) {
        if (configuration.debug) {
            body(env.messager)
        }
    }

    fun getClassDefinition(mirror: TypeMirror, generics: List<ClassDefinition> = emptyList(), type: StructureType = DEFAULT): ClassDefinition =
        ClassDefinition.classDefinitionFrom(this, mirror, generics, type)

    fun getClassDefinitions(mirrors: Set<TypeMirror>): Set<ClassDefinition> =
        mirrors.map { getClassDefinition(it) }.toSet()

    fun forTypeElement(name: String): TypeElement? = env.elementUtils.getTypeElement(name)

    fun isAssignable(implementation: TypeMirror, superclass: TypeMirror): Boolean = env.typeUtils.isAssignable(implementation, superclass)

    fun hasElement(type: TypeElement, element: Element): Boolean =
        when (element) {
            is ExecutableElement -> type.enclosedElements.filterIsInstance<ExecutableElement>().any { env.elementUtils.overrides(element, it, type) }
            else -> false
        }

    fun getFullName(mirror: TypeMirror): String =
        env.typeUtils.asElement(mirror)
            ?.getAnnotation(OpenApiName::class.java)
            ?.value
            ?.let { mirror.toString().substringBeforeLast(".") + "." + it }
            ?: mirror.toString().substringBefore("<")

    /* Extension methods, should be replaced by context receivers in the future */

    fun TypeMirror.toClassDefinition(
        generics: List<ClassDefinition> = emptyList(),
        type: StructureType = DEFAULT
    ): ClassDefinition = getClassDefinition(this, generics, type)

    fun TypeMirror.getSimpleName(): String =
        getFullName().substringAfterLast(".")

    @JvmName("getFullNameExt")
    fun TypeMirror.getFullName(): String =
        getFullName(this)

    fun <A : Annotation> A.getClassDefinitions(supplier: A.() -> Array<out KClass<*>>): Set<ClassDefinition> =
        getTypeMirrors(supplier)
            .map { it.toClassDefinition() }
            .toSet()

    fun <A : Annotation> A.getClassDefinition(supplier: A.() -> KClass<*>): ClassDefinition =
        getTypeMirror(supplier).toClassDefinition()

}

