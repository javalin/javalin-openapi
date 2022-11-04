package io.javalin.openapi

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION

@Target(CLASS, FIELD, FUNCTION)
@Retention(SOURCE)
annotation class JsonSchema

