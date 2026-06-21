package io.javalin.openapi.dynamic

import io.javalin.introspection.TypeIntrospector
import io.javalin.introspection.runtime.ReflectionTypeIntrospector
import io.javalin.openapi.experimental.IntrospectorSchemaContext
import io.javalin.openapi.experimental.SimpleType
import io.javalin.openapi.experimental.defaults.createDefaultSimpleTypeMappings

class ReflectionSchemaContext(
    simpleTypeMappings: Map<String, SimpleType> = createDefaultSimpleTypeMappings(),
) : IntrospectorSchemaContext(simpleTypeMappings) {

    override val introspector: TypeIntrospector = ReflectionTypeIntrospector()

}
