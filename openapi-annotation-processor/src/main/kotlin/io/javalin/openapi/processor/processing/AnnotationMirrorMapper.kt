package io.javalin.openapi.processor.processing

import io.javalin.openapi.processor.OpenApiAnnotationProcessor
import java.util.AbstractMap.SimpleEntry
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic.Kind.ERROR
import kotlin.collections.Map.Entry

internal open class AnnotationMirrorMapper protected constructor(protected val mirror: AnnotationMirror) {

    private fun getEntry(key: String): Entry<ExecutableElement, AnnotationValue> =
        mirror.elementValues.entries.firstOrNull { (element, _) -> element.simpleName.contentEquals(key) }
            ?: mirror.annotationType.asElement().enclosedElements.firstOrNull { it.simpleName.contentEquals(key) }
                ?.let {
                    val executableElement = it.accept(ExecutableVisitor(), null)
                    SimpleEntry(executableElement, executableElement.defaultValue)
                }
                ?: run {
                    val classSymbol = mirror.annotationType.asElement()
                    val sourceFile = classSymbol.javaClass.getField("sourcefile").get(classSymbol)
                    val classFile = classSymbol.javaClass.getField("classfile").get(classSymbol)

                    val messager = OpenApiAnnotationProcessor.messager
                    messager.printMessage(ERROR, "Missing '$key' property in @OpenApi annotation.")
                    messager.printMessage(ERROR, "Source file: $sourceFile")
                    messager.printMessage(ERROR, "Class file: $classFile")
                    messager.printMessage(ERROR, "Defined properties:")

                    mirror.elementValues.forEach { (executableElement, annotationValue) ->
                        messager.printMessage(ERROR, "  - ${executableElement.simpleName} = ${annotationValue.value}")
                    }

                    throw IllegalStateException("Missing '$key' property in @OpenApi annotation")
                }

    protected fun getValue(key: String): Any =
        getEntry(key).value.value

    protected fun <R> getAnnotation(key: String, function: (AnnotationMirror) -> R): R =
        function(getEntry(key).value.accept(AnnotationVisitor(), null))

    protected fun <T, R> getArray(key: String, type: Class<T>, mapper: (T) -> R): List<R> =
        getArray(key, type).map(mapper)

    protected fun <T> getArray(key: String, @Suppress("UNUSED_PARAMETER") type: Class<T>): List<T> =
        getEntry(key).value.accept(ArrayVisitor(), null)

    protected fun getType(key: String): TypeMirror =
        getEntry(key).value.accept(ClassVisitor(), null)

    protected fun getString(key: String): String =
        getValue(key).toString()

    protected fun getBoolean(key: String) =
        getString(key).toBoolean()

}