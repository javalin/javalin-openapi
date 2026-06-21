package io.javalin.openapi.ksp

import com.google.devtools.ksp.processing.Resolver
import io.javalin.introspection.TypeIntrospector
import io.javalin.introspection.ksp.KspTypeIntrospector
import io.javalin.openapi.experimental.IntrospectorSchemaContext
import io.javalin.openapi.experimental.SimpleType
import io.javalin.openapi.experimental.defaults.createDefaultSimpleTypeMappings

class KspSchemaContext(
    resolver: Resolver,
    simpleTypeMappings: Map<String, SimpleType> = createDefaultSimpleTypeMappings(),
) : IntrospectorSchemaContext(simpleTypeMappings) {

    override val introspector: TypeIntrospector = KspTypeIntrospector(resolver)

}
