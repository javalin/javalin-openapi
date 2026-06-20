package io.javalin.openapi.experimental

import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

/** The annotation-processing token carried in [OpenApiType.handle]. */
data class OpenApiTypeHandle(
    val mirror: TypeMirror,
    val source: Element
)

@OptIn(InternalOpenApiTypeApi::class)
val OpenApiType.mirror: TypeMirror
    get() = (handle as OpenApiTypeHandle).mirror

@OptIn(InternalOpenApiTypeApi::class)
val OpenApiType.source: Element
    get() = (handle as OpenApiTypeHandle).source
