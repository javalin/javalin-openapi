package io.javalin.openapi

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.reflect.KClass

@Target(CLASS)
@Retention(SOURCE)
annotation class JsonSchema(
    /**
     * By default, each usage of @JsonSchema annotation results in generated `/json-schemas/{type qualifier}` resource file.
     * If for some reason you need to use @JsonSchema in your OpenAPI specification, you can disable this behaviour.
     */
    val generateResource: Boolean = true,
    /**
     * By default, all non fields are marked as required.
     * You can disable this behaviour for given type using this property
     */
    val requireNonNulls: Boolean = true
)

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, FIELD)
@Retention(SOURCE)
annotation class OneOf(
    /** List of associated classes to list */
    vararg val value: KClass<*>
)

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, FIELD)
@Retention(SOURCE)
annotation class AnyOf(
    /** List of associated classes to list */
    vararg val value: KClass<*>
)

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, FIELD)
@Retention(SOURCE)
annotation class AllOf(
    /** List of associated classes to list */
    vararg val value: KClass<*>
)

/** Allows you to add custom properties to your schemes */
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, FIELD)
@Retention(SOURCE)
@Repeatable
annotation class Custom(
    /* Define name of key for custom property */
    val name: String,
    /* Define value of custom property */
    val value: String
)

/** Allows you to create custom annotations for a group of custom properties */
@Target(ANNOTATION_CLASS)
@Retention(SOURCE)
annotation class CustomAnnotation

enum class Combinator(val propertyName: String) {
    ONE_OF("oneOf"),
    ANY_OF("anyOf"),
    ALL_OF("allOf")
}

data class JsonSchemaResource(
    val name: String,
    val content: String
)

class JsonSchemaLoader {

    fun loadGeneratedSchemes(): Collection<JsonSchemaResource> =
        JsonSchemaLoader::class.java.getResourceAsStream("/json-schemes/")
            ?.readAllBytes()
            ?.decodeToString()
            ?.split("\n")
            ?.asSequence()
            ?.map { it.trim() }
            ?.map { it to JsonSchemaLoader::class.java.getResourceAsStream("/json-schemes/$it")!! }
            ?.map { (name, source) -> JsonSchemaResource(name, source.reader().readText()) }
            ?.toList()
            ?: emptyList()

}