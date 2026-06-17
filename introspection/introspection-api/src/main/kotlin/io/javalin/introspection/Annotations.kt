package io.javalin.introspection

/**
 * Backend-agnostic, name/value-based access to annotations. Deliberately avoids handing back typed annotation
 * instances or member lambdas — those can't be honored on KSP (no JVM instances). Class-valued members are read
 * through [memberValues] (resolved to [ClassDefinition]), so [resolveClass]/[resolveClasses] are derived for free.
 */
interface Annotations {

    /** True if an annotation with this simple name is present (match e.g. `@NotNull` without depending on it). */
    fun hasNamed(simpleName: String): Boolean

    /**
     * Members of [annotationType] (with defaults) as a name→value map, or null if absent.
     * Class members → [ClassDefinition], enum members → constant name, nested annotations → name→value map, arrays → list.
     */
    fun memberValues(annotationType: Class<out Annotation>): Map<String, Any?>?

    /** True if an annotation of [annotationType] is present. */
    fun has(annotationType: Class<out Annotation>): Boolean =
        memberValues(annotationType) != null

    /** Resolve a class-valued member to a [ClassDefinition]. */
    fun resolveClass(annotationType: Class<out Annotation>, member: String): ClassDefinition? =
        memberValues(annotationType)?.get(member) as? ClassDefinition

    /** Resolve a class-array-valued member to [ClassDefinition]s. */
    fun resolveClasses(annotationType: Class<out Annotation>, member: String): List<ClassDefinition> =
        (memberValues(annotationType)?.get(member) as? List<*>)?.filterIsInstance<ClassDefinition>() ?: emptyList()
}
