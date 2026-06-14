package io.javalin.introspection.test

import io.javalin.introspection.runtime.ReflectionTypeIntrospector
import io.javalin.introspection.test.fixtures.Account
import io.javalin.introspection.test.fixtures.Address
import io.javalin.introspection.test.fixtures.Color
import io.javalin.introspection.test.fixtures.Derived
import io.javalin.introspection.test.fixtures.Holder
import io.javalin.introspection.test.fixtures.Ref
import io.javalin.introspection.test.fixtures.Refs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class IntrospectionParityTest {

    private val runtime = ReflectionTypeIntrospector()

    private fun assertParity(type: KClass<*>) {
        val fqn = type.qualifiedName!!
        val runtimeSnapshot = snapshot(runtime, runtime.introspect(type.java))
        val japSnapshot = withJap { jap, elements -> snapshot(jap, jap.introspect(elements.getTypeElement(fqn).asType())) }
        assertThat(japSnapshot).isEqualTo(runtimeSnapshot)
    }

    @Test fun `scalar, collection, map and nested members match across backends`() = assertParity(Account::class)

    @Test fun `plain object members match across backends`() = assertParity(Address::class)

    @Test fun `enum constants match across backends`() = assertParity(Color::class)

    @Test fun `inherited public getters and private superclass fields match across backends`() = assertParity(Derived::class)

    @Test
    fun `Class-valued annotation members resolve identically across backends`() {
        val runtimeAnnotations = runtime.annotations(runtime.introspect(Holder::class.java))
        val runtimeRef = runtimeAnnotations.classValue(Ref::class.java) { value }?.fullName
        val runtimeRefs = runtimeAnnotations.classValues(Refs::class.java) { value }.map { it.fullName }

        val (japRef, japRefs) = withJap { jap, elements ->
            val annotations = jap.annotations(jap.introspect(elements.getTypeElement(Holder::class.qualifiedName).asType()))
            annotations.classValue(Ref::class.java) { value }?.fullName to annotations.classValues(Refs::class.java) { value }.map { it.fullName }
        }

        assertThat(japRef).isEqualTo(runtimeRef).isEqualTo(Address::class.java.name)
        assertThat(japRefs).isEqualTo(runtimeRefs).containsExactly(Address::class.java.name, Color::class.java.name)
    }

    @Test
    fun `annotation value maps match across backends`() {
        val runtimeValues = runtime.annotations(runtime.introspect(Holder::class.java)).values(Ref::class.java)
        val japValues = withJap { jap, elements ->
            jap.annotations(jap.introspect(elements.getTypeElement(Holder::class.qualifiedName).asType())).values(Ref::class.java)
        }

        val runtimeRefType = (runtimeValues!!.getValue("value") as io.javalin.introspection.ClassDefinition).fullName
        val japRefType = (japValues!!.getValue("value") as io.javalin.introspection.ClassDefinition).fullName
        assertThat(japRefType).isEqualTo(runtimeRefType).isEqualTo(Address::class.java.name)
    }
}
