package io.javalin.introspection.ksp.fixtures

import kotlin.reflect.KClass

enum class Color { RED, GREEN }

annotation class Ref(val value: KClass<*>)

class Address(val city: String, val zip: String)

@Ref(Address::class)
class Account(
    val id: String,
    val age: Int,
    val color: Color,
    val address: Address?,
    val tags: List<String>,
    val meta: Map<String, Int>,
)
