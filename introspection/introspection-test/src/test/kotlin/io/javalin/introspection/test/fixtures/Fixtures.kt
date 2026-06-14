package io.javalin.introspection.test.fixtures

import kotlin.reflect.KClass

class Address(val city: String, val zip: String)

enum class Color { RED, GREEN }

class Account(
    val id: String,
    val age: Int,
    val color: Color,
    val address: Address?,
    val tags: List<String>,
    val meta: Map<String, Int>,
)

open class Base(val baseField: String) {
    val computed: String get() = ""
    protected val secret: String get() = ""
}

class Derived(val own: String) : Base("")

annotation class Ref(val value: KClass<*>)

annotation class Refs(vararg val value: KClass<*>)

@Ref(Address::class)
@Refs(Address::class, Color::class)
class Holder
