package io.javalin.openapi.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotated
import io.javalin.introspection.AnnotationSet
import io.javalin.introspection.PropertyView
import io.javalin.introspection.TypeIntrospector
import io.javalin.introspection.ksp.KspTypeIntrospector
import io.javalin.openapi.OpenApiByFields
import io.javalin.openapi.experimental.IntrospectorSchemaContext
import io.javalin.openapi.experimental.OpenApiType
import io.javalin.openapi.experimental.SimpleType
import io.javalin.openapi.experimental.defaults.createDefaultSimpleTypeMappings

internal class KspSchemaContext(
    resolver: Resolver,
    private val logger: KSPLogger,
    simpleTypeMappings: Map<String, SimpleType> = createDefaultSimpleTypeMappings(),
) : IntrospectorSchemaContext(simpleTypeMappings) {

    private val kspTypeIntrospector = KspTypeIntrospector(resolver)
    override val introspector: TypeIntrospector = kspTypeIntrospector

    fun annotationsOf(annotated: KSAnnotated): AnnotationSet =
        kspTypeIntrospector.annotationsOf(annotated)

    override fun propertiesOf(type: OpenApiType): List<PropertyView> {
        if (annotationsOf(type).find(OpenApiByFields::class.java)?.boolean("only") == true) {
            logger.error(
                "KSP does not support @OpenApiByFields(only = true). " +
                    "Use APT/Kapt for field-only schema generation, or remove only = true."
            )
            return emptyList()
        }

        return super.propertiesOf(type)
    }

    override fun reportWarning(message: String) {
        logger.warn(message)
    }

}
