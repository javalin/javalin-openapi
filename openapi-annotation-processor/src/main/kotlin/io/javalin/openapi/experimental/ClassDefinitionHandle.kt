package io.javalin.openapi.experimental

import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

data class ClassDefinitionHandle(
    val mirror: TypeMirror,
    val source: Element
)

val ClassDefinition.mirror: TypeMirror
    get() = (handle as ClassDefinitionHandle).mirror

val ClassDefinition.source: Element
    get() = (handle as ClassDefinitionHandle).source
