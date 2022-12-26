package io.javalin.openapi.experimental

import com.google.gson.JsonObject
import io.javalin.openapi.experimental.defaults.ArrayEmbeddedTypeProcessor
import io.javalin.openapi.experimental.defaults.CompositionEmbeddedTypeProcessor
import io.javalin.openapi.experimental.defaults.DictionaryEmbeddedTypeProcessor
import io.javalin.openapi.experimental.defaults.createDefaultSimpleTypeMappings
import io.javalin.openapi.experimental.processor.generators.PropertyComposition
import javax.lang.model.element.Element
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

@RequiresOptIn
@Retention(BINARY)
@Target(CLASS, FUNCTION)
annotation class ExperimentalCompileOpenApiConfiguration

@ExperimentalCompileOpenApiConfiguration
interface OpenApiAnnotationProcessorConfigurer {
    fun configure(configuration: OpenApiAnnotationProcessorConfiguration)
}

data class SimpleType @JvmOverloads constructor(
    val type: String,
    val format: String? = null
)

class OpenApiAnnotationProcessorConfiguration {
    var debug: Boolean = false
    var propertyInSchemeFilter: PropertyInSchemeFilter? = null
    val simpleTypeMappings: MutableMap<String, SimpleType> = createDefaultSimpleTypeMappings()
    val embeddedTypeProcessors: MutableList<EmbeddedTypeProcessor> = mutableListOf(
        CompositionEmbeddedTypeProcessor(),
        ArrayEmbeddedTypeProcessor(),
        DictionaryEmbeddedTypeProcessor()
    )
}

fun interface PropertyInSchemeFilter {
    fun filter(context: AnnotationProcessorContext, type: ClassDefinition, property: Element): Boolean
}

data class EmbeddedTypeProcessorContext(
    val parentContext: AnnotationProcessorContext,
    val scheme: JsonObject,
    val references: MutableSet<ClassDefinition>,
    val type: ClassDefinition,
    val inlineRefs: Boolean = false,
    val requiresNonNulls: Boolean = true,
    val composition: PropertyComposition? = null,
    val extra: Map<String, Any?> = emptyMap()
)

fun interface EmbeddedTypeProcessor {
    fun process(context: EmbeddedTypeProcessorContext): Boolean
}
