package io.javalin.openapi

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION

@Target(CLASS, FIELD, FUNCTION)
@Retention(SOURCE)
annotation class JsonSchema

data class JsonSchemaResource(
    val name: String,
    val content: String
)

class JsonSchemaLoader {

    fun loadGeneratedSchemes(): Collection<JsonSchemaResource> =
        JsonSchemaLoader::class.java.getResourceAsStream("/json-schemes/")
            ?.readAllBytes()
            ?.decodeToString()
            ?.split(System.lineSeparator())
            ?.asSequence()
            ?.map { it.trim() }
            ?.map { it to JsonSchemaLoader::class.java.getResourceAsStream("/json-schemes/$it")!! }
            ?.map { (name, source) -> JsonSchemaResource(name, source.readBytes().decodeToString()) }
            ?.toList()
            ?: emptyList()

}