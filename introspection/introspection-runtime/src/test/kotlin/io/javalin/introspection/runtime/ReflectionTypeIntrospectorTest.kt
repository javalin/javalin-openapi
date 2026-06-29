package io.javalin.introspection.runtime

import io.javalin.introspection.Accessor
import io.javalin.introspection.PropertyView
import io.javalin.introspection.StructureType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class ReflectionTypeIntrospectorTest {

    private val introspector = ReflectionTypeIntrospector()

    private fun props(type: Class<*>): Map<String, PropertyView> =
        introspector.introspect(type).getProperties().associateBy { it.name }

    @Test
    fun `resolves a class into the shared model`() {
        val account = introspector.introspect(Account::class.java)
        assertThat(account.simpleName).isEqualTo("Account")
        assertThat(account.fullName).isEqualTo("io.javalin.introspection.runtime.Account")
        assertThat(account.structureType).isEqualTo(StructureType.DEFAULT)
    }

    @Test
    fun `exposes getter members under logical names with boxed primitives and structural nullability`() {
        val props = props(Account::class.java)

        assertThat(props.keys).contains("id", "age", "name", "color", "address", "tags", "meta")
        assertThat(props.getValue("id").accessor).isEqualTo(Accessor.GETTER)

        assertThat(props.getValue("age").type.fullName).isEqualTo("java.lang.Integer")
        assertThat(props.getValue("age").nullable).isFalse()
        assertThat(props.getValue("name").nullable).isTrue()
    }

    @Test
    fun `resolves collections, maps and nested types`() {
        val props = props(Account::class.java)

        assertThat(props.getValue("tags").type.structureType).isEqualTo(StructureType.ARRAY)
        assertThat(props.getValue("tags").type.fullName).isEqualTo("java.lang.String")

        val meta = props.getValue("meta").type
        assertThat(meta.structureType).isEqualTo(StructureType.DICTIONARY)
        assertThat(meta.generics.map { it.fullName }).containsExactly("java.lang.String", "java.lang.Integer")

        assertThat(props.getValue("address").type.fullName).isEqualTo(Address::class.java.name)
    }

    @Test
    fun `exposes annotations without applying policy`() {
        val id = props(Account::class.java).getValue("id")
        assertThat(id.annotations.contains(Nn::class.java)).isTrue()
        assertThat(id.annotations.contains("Nn")).isTrue()
        assertThat(props(Account::class.java).getValue("name").annotations.contains(Nn::class.java)).isFalse()
    }

    @Test
    fun `reads enum constants raw`() {
        val color = introspector.introspect(Color::class.java)
        assertThat(color.isEnum()).isTrue()
        assertThat(color.getEnumConstants()?.map { it.name }).containsExactly("RED", "GREEN")
        assertThat(introspector.introspect(Account::class.java).getEnumConstants()).isNull()
    }

    @Test
    fun `tags each property with its backing accessors`() {
        class FieldBag {
            @JvmField val tag: String = ""
            fun getName(): String = ""
        }

        val props = props(FieldBag::class.java)
        assertThat(props.getValue("name").accessor).isEqualTo(Accessor.GETTER)
        assertThat(props.getValue("tag").accessor).isEqualTo(Accessor.FIELD)
    }

    @Test
    fun `resolves Class-valued annotation members into ClassDefinitions`() {
        val annotations = introspector.introspect(Holder::class.java).getAnnotations()

        assertThat(annotations.find(Ref::class.java)?.classValue("value")?.fullName).isEqualTo(Address::class.java.name)
        assertThat(annotations.find(Refs::class.java)?.classValues("value").orEmpty().map { it.fullName })
            .containsExactly(Address::class.java.name, Color::class.java.name)
    }

    @Test
    fun `reads all annotation members into a neutral value map`() {
        val annotations = introspector.introspect(Holder::class.java).getAnnotations()

        val values = annotations.find(Mixed::class.java)?.values!!
        assertThat(values["name"]).isEqualTo("x")
        assertThat(values["count"]).isEqualTo(3)
        assertThat((values["type"] as io.javalin.introspection.ClassDefinition).fullName).isEqualTo(Address::class.java.name)
        assertThat(values["shade"]).isEqualTo("RED")
        assertThat(annotations.find(java.lang.Deprecated::class.java)?.values).isNull()
    }
}

private class Address

private enum class Color { RED, GREEN }

private annotation class Nn

private class Account {
    @Nn
    fun getId(): String = ""
    fun getAge(): Int = 0
    fun getName(): String = ""
    fun getColor(): Color = Color.RED
    fun getAddress(): Address? = null
    fun getTags(): List<String> = emptyList()
    fun getMeta(): Map<String, Int> = emptyMap()
}

private annotation class Ref(val value: KClass<*>)
private annotation class Refs(vararg val value: KClass<*>)
private annotation class Mixed(val name: String, val count: Int, val type: KClass<*>, val shade: Color)

@Ref(Address::class)
@Refs(Address::class, Color::class)
@Mixed(name = "x", count = 3, type = Address::class, shade = Color.RED)
private class Holder
