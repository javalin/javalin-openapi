package io.javalin.openapi.dynamic

import io.javalin.openapi.dynamic.fixtures.Account
import io.javalin.openapi.dynamic.fixtures.Address
import io.javalin.openapi.dynamic.fixtures.FieldsDto
import io.javalin.openapi.dynamic.fixtures.Role
import io.javalin.openapi.dynamic.fixtures.SnakeCaseDto
import io.javalin.openapi.experimental.StructureType
import io.javalin.openapi.experimental.processor.generators.Property
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ReflectiveTypeIntrospectorTest {

    private val introspector = ReflectiveTypeIntrospector()

    private fun accountProperties(): Map<String, Property> =
        introspector.properties(introspector.introspect(Account::class.java)).associateBy { it.name }

    @Test
    fun `resolves a class into the shared ClassDefinition model`() {
        val account = introspector.introspect(Account::class.java)

        assertThat(account.simpleName).isEqualTo("Account")
        assertThat(account.fullName).isEqualTo("io.javalin.openapi.dynamic.fixtures.Account")
        assertThat(account.structureType).isEqualTo(StructureType.DEFAULT)
    }

    @Test
    fun `lists scalar properties with required flags, renames, and ignores`() {
        val props = accountProperties()

        assertThat(props.keys).contains("id", "age", "name", "e_mail")
        assertThat(props.keys).doesNotContain("email", "secret", "class")

        assertThat(props.getValue("id").type.fullName).isEqualTo("java.lang.String")
        assertThat(props.getValue("id").required).isTrue() // @NotNull

        assertThat(props.getValue("age").type.fullName).isEqualTo("java.lang.Integer") // boxed primitive
        assertThat(props.getValue("age").required).isTrue() // primitive ⇒ non-null

        assertThat(props.getValue("name").required).isFalse()
    }

    @Test
    fun `resolves collections and maps into structure types`() {
        val props = accountProperties()

        val tags = props.getValue("tags").type
        assertThat(tags.structureType).isEqualTo(StructureType.ARRAY)
        assertThat(tags.fullName).isEqualTo("java.lang.String")

        val meta = props.getValue("meta").type
        assertThat(meta.structureType).isEqualTo(StructureType.DICTIONARY)
        assertThat(meta.generics.map { it.fullName }).containsExactly("java.lang.String", "java.lang.Integer")
    }

    @Test
    fun `descends into nested object types`() {
        val address = accountProperties().getValue("address").type
        assertThat(address.fullName).isEqualTo(Address::class.java.name)

        val addressProperties = introspector.properties(address).associateBy { it.name }
        assertThat(addressProperties.keys).containsExactlyInAnyOrder("city", "zip")
    }

    @Test
    fun `reads enum constants with renames`() {
        val role = accountProperties().getValue("role").type

        assertThat(introspector.isEnum(role)).isTrue()
        assertThat(introspector.enumConstants(role)).containsExactly("ADMIN", "regular_user")
        assertThat(introspector.enumConstants(introspector.introspect(Role::class.java)))
            .containsExactly("ADMIN", "regular_user")
    }

    @Test
    fun `captures OpenApiDescription into property extra`() {
        assertThat(accountProperties().getValue("label").extra["description"])
            .isEqualTo("Human readable label")
    }

    @Test
    fun `applies the configured naming strategy`() {
        val props = introspector.properties(introspector.introspect(SnakeCaseDto::class.java)).associateBy { it.name }
        assertThat(props.keys).contains("first_name")
    }

    @Test
    fun `reads fields and honors visibility when OpenApiByFields is present`() {
        val props = introspector.properties(introspector.introspect(FieldsDto::class.java)).associateBy { it.name }
        assertThat(props.keys).containsExactly("publicField")
    }

    @Test
    fun `rejects non-reflection native tokens`() {
        assertThrows<IllegalArgumentException> {
            introspector.introspect("not a type" as Any)
        }
    }
}
