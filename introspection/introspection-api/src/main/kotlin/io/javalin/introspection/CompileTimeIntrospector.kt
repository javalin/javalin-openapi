package io.javalin.introspection

interface CompileTimeIntrospector : TypeIntrospector {

    fun typesAnnotatedWith(
        annotationType: Class<out Annotation>,
        assignableTo: ClassDefinition? = null,
    ): List<ClassDefinition>

}
