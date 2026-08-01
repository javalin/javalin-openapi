package io.javalin.openapi.dynamic

import com.fasterxml.jackson.databind.JsonNode
import io.javalin.openapi.experimental.StructureType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ReflectionSchemaContextTest {

    private val schemaContext = ReflectionSchemaContext()

    private fun schemaOf(type: Class<*>): JsonNode =
        schemaContext.componentSchema(schemaContext.introspect(type)).json

    private fun propertyNames(type: Class<*>): List<String> =
        schemaOf(type).path("properties").fieldNames().asSequence().toList()

    private fun accountSchema(): JsonNode =
        schemaOf(Account::class.java)

    private fun accountProperties(): JsonNode =
        accountSchema().path("properties")

    @Test
    fun `resolves a class into the shared OpenApiType model`() {
        val account = schemaContext.introspect(Account::class.java)

        assertThat(account.simpleName).isEqualTo("Account")
        assertThat(account.fullName).isEqualTo("io.javalin.openapi.dynamic.Account")
        assertThat(account.structureType).isEqualTo(StructureType.DEFAULT)
    }

    @Test
    fun `honors renamed account properties`() {
        val properties = accountProperties()

        assertThat(properties.has("e_mail")).isTrue()
        assertThat(properties.has("email")).isFalse()
    }

    @Test
    fun `omits ignored account properties`() {
        val properties = accountProperties()

        assertThat(properties.has("secret")).isFalse()
    }

    @Test
    fun `omits synthetic account properties`() {
        val properties = accountProperties()

        assertThat(properties.has("class")).isFalse()
    }

    @Test
    fun `marks required account properties`() {
        val account = accountSchema()

        assertThat(account.path("required").map { it.asText() }).containsExactlyInAnyOrder("id", "age")
    }

    @Test
    fun `renders account property descriptions`() {
        val properties = accountProperties()

        assertThat(properties.path("label").path("description").asText()).isEqualTo("Human readable label")
    }

    @Test
    fun `renders collection properties`() {
        val properties = accountProperties()

        assertThat(properties.path("tags").path("type").asText()).isEqualTo("array")
        assertThat(properties.path("tags").path("items").path("type").asText()).isEqualTo("string")
    }

    @Test
    fun `renders map properties`() {
        val properties = accountProperties()

        assertThat(properties.path("meta").path("type").asText()).isEqualTo("object")
        assertThat(properties.path("meta").path("additionalProperties").path("type").asText()).isEqualTo("integer")
    }

    @Test
    fun `references nested object properties`() {
        val properties = accountProperties()

        assertThat(properties.path("address").path($$"$ref").asText()).isEqualTo("#/components/schemas/Address")
    }

    @Test
    fun `renders nested object properties`() {

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
    fun `includes fluent accessors annotated with OpenApiName`() {
        assertThat(propertyNames(FluentOpenApiNameDto::class.java)).containsExactly("age")
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
