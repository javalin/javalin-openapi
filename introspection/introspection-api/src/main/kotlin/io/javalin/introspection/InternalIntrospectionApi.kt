package io.javalin.introspection

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Target agnostic code must not touch that part of the API.",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class InternalIntrospectionApi
