package io.javalin.openapi

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.reflect.KClass

@Target(CLASS, FIELD, FUNCTION)
@Retention(SOURCE)
annotation class JsonSchema

@Target(CLASS, FIELD, FUNCTION)
@Retention(SOURCE)
annotation class OneOf(
    vararg val value: KClass<*>
)

@Target(CLASS, FIELD, FUNCTION)
@Retention(SOURCE)
annotation class AnyOf(
    vararg val value: KClass<*>
)

@Target(CLASS, FIELD, FUNCTION)
@Retention(SOURCE)
annotation class AllOf(
    vararg val value: KClass<*>
)

@Target(FIELD, FUNCTION)
@Retention(SOURCE)
@Repeatable
annotation class Custom(
    val name: String,
    val value: String
)

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
            ?.map { (name, source) -> JsonSchemaResource(name, source.readBytes().decodeToString()) }
            ?.toList()
            ?: emptyList()

}