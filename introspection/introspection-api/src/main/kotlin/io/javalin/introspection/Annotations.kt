package io.javalin.introspection

import kotlin.reflect.KClass

interface Annotations {
    fun <A : Annotation> find(annotationType: Class<A>): A?
    fun hasBySimpleName(simpleName: String): Boolean

    /** Resolve a `KClass`-valued member into a [ClassDefinition], hiding the compile-time `MirroredTypeException`. */
    fun <A : Annotation> classValue(annotationType: Class<A>, member: A.() -> KClass<*>): ClassDefinition?

    fun <A : Annotation> classValues(annotationType: Class<A>, member: A.() -> Array<out KClass<*>>): List<ClassDefinition>

    /** Members of [annotationType] as a neutral map: `Class`→[ClassDefinition], enum→name, array→list. */
    fun values(annotationType: Class<out Annotation>): Map<String, Any?>?
}
