package io.javalin.introspection.test.fixtures

import kotlin.reflect.KClass

class Address(val city: String, val zip: String)

enum class Color { RED, GREEN }

class Box<T>(val value: T)

class Bounded<T : Address>(val value: T, val many: List<T>)

@JvmRecord
data class Point(val x: Int, val label: String, val tags: List<String>)

class Account(
    val id: String,
    val age: Int,
    val color: Color,
    val address: Address?,
    val tags: List<String>,
    val meta: Map<String, Int>,
    val bounded: Box<out Number>,
)

open class Base(val baseField: String) {
    val computed: String get() = ""
    protected val secret: String get() = ""
}

class Derived(val own: String) : Base("")

/** Extends a Java base in another package that has a package-private field, to exercise cross-package field inheritance. */
class CrossPackageChild : io.javalin.introspection.test.sub.PackagePrivateBase()

class WithTransient(@Transient val skipped: String, val kept: String)

class Tricky {
    fun getName(): String = ""
    fun issue(): String = ""
    fun getaway(): String = ""
    fun getResult() {}
}

annotation class Marker

class Annotated(@get:Marker val tagged: String, val plain: String)

annotation class Meta(val note: String)

annotation class Outer(val meta: Meta)

@Outer(Meta("x"))
class Wrapped

annotation class MetaMarker

@MetaMarker
annotation class Tagged(val label: String)

@Tagged("x")
class Scanned

annotation class Flags(val ints: IntArray)

@Flags(ints = [1, 2, 3])
class Flagged

annotation class Ref(val value: KClass<*>)

annotation class Refs(vararg val value: KClass<*>)

@Ref(Address::class)
@Refs(Address::class, Color::class)
class Holder
