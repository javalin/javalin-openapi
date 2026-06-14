package io.javalin.introspection

import kotlin.reflect.KClass

interface Annotations {
    fun <A : Annotation> find(annotationType: Class<A>): A?

    /** True if an annotation with this simple name is present (match e.g. `@NotNull` without depending on it). */
    fun hasNamed(simpleName: String): Boolean

    /** Resolve a class-valued member (e.g. `value = Foo::class`) to a [ClassDefinition]. */
    fun <A : Annotation> resolveType(annotationType: Class<A>, member: A.() -> KClass<*>): ClassDefinition?

    fun <A : Annotation> resolveTypes(annotationType: Class<A>, member: A.() -> Array<out KClass<*>>): List<ClassDefinition>

    /** Members of [annotationType] as a name→value map (class members resolved to [ClassDefinition], enums to their name). */
    fun memberValues(annotationType: Class<out Annotation>): Map<String, Any?>?
}
