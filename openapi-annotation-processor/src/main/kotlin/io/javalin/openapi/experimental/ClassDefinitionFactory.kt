package io.javalin.openapi.experimental

import io.javalin.introspection.InternalIntrospectionApi
import io.javalin.introspection.jap.JapTypeIntrospector
import io.javalin.openapi.experimental.processor.shared.mapType
import io.javalin.openapi.experimental.processor.shared.objectType
import javax.lang.model.type.TypeMirror
import io.javalin.introspection.ClassDefinition as RawType
import io.javalin.introspection.StructureType as RawStructureType

/**
 * Resolves a [TypeMirror] into the OpenAPI [ClassDefinition] model. Structure resolution (array/map/collection/
 * generics/primitive/typevar) is delegated to the shared [JapTypeIntrospector]; names + handle stay AP-specific.
 */
fun classDefinitionFrom(context: AnnotationProcessorContext, mirror: TypeMirror): ClassDefinition =
    context.toExperimental(JapTypeIntrospector(context.types, context.env.elementUtils).introspect(mirror))

@OptIn(InternalIntrospectionApi::class)
internal fun AnnotationProcessorContext.toExperimental(raw: RawType): ClassDefinition =
    inContext {
        val mirror = raw.source as TypeMirror
        ClassDefinition(
            simpleName = mirror.getSimpleName(),
            fullName = mirror.getFullName(),
            generics = raw.generics.map { toExperimental(it) },
            structureType = StructureType.valueOf(raw.structureType.name),
            handle = ClassDefinitionHandle(
                mirror = mirror,
                source = if (raw.structureType == RawStructureType.DICTIONARY) mapType() else (types.asElement(mirror) ?: objectType())
            )
        )
    }
