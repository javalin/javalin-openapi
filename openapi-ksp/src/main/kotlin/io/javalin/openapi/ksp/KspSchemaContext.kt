package io.javalin.openapi.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import io.javalin.introspection.AnnotationSet
import io.javalin.introspection.TypeIntrospector
import io.javalin.introspection.ksp.KspTypeIntrospector
import io.javalin.openapi.experimental.IntrospectorSchemaContext
import io.javalin.openapi.experimental.SimpleType
import io.javalin.openapi.experimental.defaults.createDefaultSimpleTypeMappings

class KspSchemaContext(
    resolver: Resolver,
    simpleTypeMappings: Map<String, SimpleType> = createDefaultSimpleTypeMappings(),
) : IntrospectorSchemaContext(simpleTypeMappings) {

    private val kspTypeIntrospector = KspTypeIntrospector(resolver)
    override val introspector: TypeIntrospector = kspTypeIntrospector

    fun annotationsOf(annotated: KSAnnotated): AnnotationSet =
        kspTypeIntrospector.annotationsOf(annotated)

}
