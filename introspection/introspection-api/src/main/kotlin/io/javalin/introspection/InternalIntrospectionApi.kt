package io.javalin.introspection

/**
 * Opt-in marker for [ClassDefinition.source] / [PropertyView.source] — the backend-native token (`Class`,
 * `TypeMirror`, `KSType`, ...). It is only meaningful to the backend that produced it; backend-agnostic code must
 * never cast it. Opt in only inside a backend adapter that knows the concrete token type.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "source is a backend-native token; only the producing backend may read/cast it. Agnostic code must not touch it.",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class InternalIntrospectionApi
