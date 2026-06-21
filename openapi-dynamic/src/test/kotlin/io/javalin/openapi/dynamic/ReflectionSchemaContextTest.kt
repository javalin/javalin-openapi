package io.javalin.openapi.dynamic

import com.fasterxml.jackson.databind.JsonNode
import io.javalin.openapi.dynamic.fixtures.Account
import io.javalin.openapi.dynamic.fixtures.Address
import io.javalin.openapi.dynamic.fixtures.FieldsDto
import io.javalin.openapi.dynamic.fixtures.Role
import io.javalin.openapi.dynamic.fixtures.SnakeCaseDto
import io.javalin.openapi.dynamic.fixtures.TransientDto
import io.javalin.openapi.experimental.StructureType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ReflectionSchemaContextTest {

    private val schemaContext = ReflectionSchemaContext()

    private fun schemaOf(type: Class<*>): JsonNode =
        schemaContext.componentSchema(schemaContext.introspect(type)).json

    private fun propertyNames(type: Class<*>): List<String> =
        schemaOf(type).path("properties").fieldNames().asSequence().toList()

    @Test
    fun `resolves a class into the shared OpenApiType model`() {
        val account = schemaContext.introspect(Account::class.java)

        assertThat(account.simpleName).isEqualTo("Account")
        assertThat(account.fullName).isEqualTo("io.javalin.openapi.dynamic.fixtures.Account")
        assertThat(account.structureType).isEqualTo(StructureType.DEFAULT)
    }

    @Test
    fun `applies renames, ignores, required flags and descriptions through the shared generator`() {
        val account = schemaOf(Account::class.java)
        val properties = account.path("properties")

        assertThat(properties.fieldNames().asSequence().toList()).contains("id", "age", "name", "e_mail")
        assertThat(properties.has("email")).isFalse()
        assertThat(properties.has("secret")).isFalse()
        assertThat(properties.has("class")).isFalse()

        assertThat(account.path("required").map { it.asText() }).containsExactlyInAnyOrder("id", "age")
        assertThat(properties.path("label").path("description").asText()).isEqualTo("Human readable label")
    }

    @Test
    fun `resolves collections, maps and nested objects`() {
        val properties = schemaOf(Account::class.java).path("properties")

        assertThat(properties.path("tags").path("type").asText()).isEqualTo("array")
        assertThat(properties.path("tags").path("items").path("type").asText()).isEqualTo("string")
        assertThat(properties.path("meta").path("type").asText()).isEqualTo("object")
        assertThat(properties.path("meta").path("additionalProperties").path("type").asText()).isEqualTo("integer")
        assertThat(properties.path("address").path($$"$ref").asText()).isEqualTo("#/components/schemas/Address")

        assertThat(propertyNames(Address::class.java)).containsExactlyInAnyOrder("city", "zip")
    }

    @Test
    fun `reads enum constants with renames`() {
        assertThat(schemaContext.isEnum(schemaContext.introspect(Role::class.java))).isTrue()

        val role = schemaOf(Role::class.java)
        assertThat(role.path("type").asText()).isEqualTo("string")
        assertThat(role.path("enum").map { it.asText() }).containsExactly("ADMIN", "regular_user")
    }

    @Test
    fun `applies the configured naming strategy`() {
        assertThat(propertyNames(SnakeCaseDto::class.java)).contains("first_name")
    }

    @Test
    fun `reads fields and honors visibility when OpenApiByFields is present`() {
        assertThat(propertyNames(FieldsDto::class.java)).containsExactly("publicField")
    }

    @Test
    fun `skips transient fields under OpenApiByFields`() {
        assertThat(propertyNames(TransientDto::class.java)).containsExactly("kept")
    }
}
