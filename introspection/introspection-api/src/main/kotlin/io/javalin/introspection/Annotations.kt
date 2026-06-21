package io.javalin.introspection

// Name/value-based on purpose: typed annotation instances and member lambdas can't be honored on KSP.
interface Annotations {

    fun hasNamed(simpleName: String): Boolean

    // Members (with defaults), or null if absent. Classes → ClassDefinition, enums → constant name, nested → map, arrays → list.
    fun memberValues(annotationType: Class<out Annotation>): Map<String, Any?>?

    fun memberValuesList(annotationType: Class<out Annotation>): List<Map<String, Any?>>

    fun all(): List<AnnotationView>

    fun has(annotationType: Class<out Annotation>): Boolean =
        memberValues(annotationType) != null

    fun resolveClass(annotationType: Class<out Annotation>, member: String): ClassDefinition? =
        memberValues(annotationType)?.get(member) as? ClassDefinition

    fun resolveClasses(annotationType: Class<out Annotation>, member: String): List<ClassDefinition> =
        (memberValues(annotationType)?.get(member) as? List<*>)?.filterIsInstance<ClassDefinition>() ?: emptyList()
}

interface AnnotationView {
    val qualifiedName: String
    val simpleName: String
    val meta: Annotations
    fun values(): Map<String, Any?>
}
