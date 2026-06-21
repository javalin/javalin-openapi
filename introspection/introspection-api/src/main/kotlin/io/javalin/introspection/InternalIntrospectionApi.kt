package io.javalin.introspection

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "source is a backend-native token; only the producing backend may read/cast it. Agnostic code must not touch it.",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class InternalIntrospectionApi
