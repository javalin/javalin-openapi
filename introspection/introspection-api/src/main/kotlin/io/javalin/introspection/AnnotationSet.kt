package io.javalin.introspection

interface AnnotationSet {

    fun all(): List<AnnotationProjection>

    fun find(type: Class<out Annotation>): AnnotationProjection?

    fun findAll(type: Class<out Annotation>): List<AnnotationProjection>

    fun contains(simpleName: String): Boolean

    fun contains(type: Class<out Annotation>): Boolean = find(type) != null

}

interface AnnotationProjection {

    val simpleName: String

    val metadata: AnnotationSet

    val values: Map<String, Any?>

    operator fun get(member: String): AnnotationValue = AnnotationValue(values[member])

}

data class AnnotationValue(private val value: Any?) {

    fun raw(): Any? = value

    fun asString(): String? = value as? String

    fun asBoolean(): Boolean? = value as? Boolean

    fun asClassDefinition(): ClassDefinition? = value as? ClassDefinition

    fun asClassDefinitions(): List<ClassDefinition> =
        asList().filterIsInstance<ClassDefinition>()

    fun asList(): List<*> = value as? List<*> ?: emptyList<Any?>()

    fun asMap(): Map<*, *>? = value as? Map<*, *>

}

class RepeatableAnnotationProjection(
    override val simpleName: String,
    override val values: Map<String, Any?>,
) : AnnotationProjection {
    override val metadata: AnnotationSet = EmptyAnnotationSet
}

private object EmptyAnnotationSet : AnnotationSet {
    override fun all(): List<AnnotationProjection> = emptyList()
    override fun find(type: Class<out Annotation>): AnnotationProjection? = null
    override fun findAll(type: Class<out Annotation>): List<AnnotationProjection> = emptyList()
    override fun contains(simpleName: String): Boolean = false
}
