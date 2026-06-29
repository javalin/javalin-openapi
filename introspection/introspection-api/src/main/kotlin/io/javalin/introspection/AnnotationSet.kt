package io.javalin.introspection

/** The annotations on a single element. Name/value-based: KSP can't honor typed annotation instances. */
interface AnnotationSet {

    fun all(): List<AnnotationView>

    fun find(type: Class<out Annotation>): AnnotationView?

    fun findAll(type: Class<out Annotation>): List<AnnotationView>

    fun contains(simpleName: String): Boolean

    fun contains(type: Class<out Annotation>): Boolean =
        find(type) != null

    companion object {
        val EMPTY: AnnotationSet = object : AnnotationSet {
            override fun all(): List<AnnotationView> = emptyList()
            override fun find(type: Class<out Annotation>): AnnotationView? = null
            override fun findAll(type: Class<out Annotation>): List<AnnotationView> = emptyList()
            override fun contains(simpleName: String): Boolean = false
        }
    }
}

interface AnnotationView {

    val simpleName: String

    val meta: AnnotationSet

    val values: Map<String, Any?>

    fun value(member: String): Any? =
        values[member]

    fun string(member: String): String? =
        value(member) as? String

    fun boolean(member: String): Boolean? =
        value(member) as? Boolean

    fun classValue(member: String): ClassDefinition? =
        value(member) as? ClassDefinition

    fun classValues(member: String): List<ClassDefinition> =
        (value(member) as? List<*>)?.filterIsInstance<ClassDefinition>().orEmpty()

    companion object {
        fun of(simpleName: String, values: Map<String, Any?>): AnnotationView =
            ValuesAnnotationView(simpleName, values)
    }
}

private class ValuesAnnotationView(
    override val simpleName: String,
    override val values: Map<String, Any?>,
) : AnnotationView {
    override val meta: AnnotationSet
        get() = AnnotationSet.EMPTY
}
