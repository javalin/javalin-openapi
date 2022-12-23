package io.javalin.openapi.experimental

import com.sun.source.util.Trees
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

interface AnnotationProcessorContext {
    val env: ProcessingEnvironment
    val roundEnv: RoundEnvironment
    val trees: Trees

    fun forTypeElement(name: String): TypeElement? = env.elementUtils.getTypeElement(name)

    fun isAssignable(implementation: TypeMirror, superclass: TypeMirror): Boolean = env.typeUtils.isAssignable(implementation, superclass)

    fun hasElement(type: TypeElement, element: Element): Boolean =
        when (element) {
            is ExecutableElement -> type.enclosedElements.filterIsInstance<ExecutableElement>().any { env.elementUtils.overrides(element, it, type) }
            else -> false
        }

}

enum class StructureType {
    DEFAULT,
    ARRAY,
    DICTIONARY
}

interface ClassDefinition {
    val simpleName: String
    val fullName: String
    val mirror: TypeMirror
    val source: Element
    val generics: List<ClassDefinition>
    val type: StructureType
    val extra: MutableList<Extra>
}

interface Extra

class CustomProperty(
    val name: String,
    val type: ClassDefinition
) : Extra

