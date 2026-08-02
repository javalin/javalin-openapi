package io.javalin.openapi.dynamic

import com.fasterxml.jackson.databind.JsonNode
import io.javalin.openapi.experimental.processor.shared.jsonMapper
import io.javalin.openapi.schema.OpenApiSchemaBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DynamicSchemaGeneratorTest {

    private val schemaContext = ReflectionSchemaContext()

    private fun JsonNode.ref(): String = path($$"$ref").asText()
    private fun JsonNode.stringArray(): List<String> = map { it.asText() }

    private fun accountDocument(): JsonNode {
        val builder = OpenApiSchemaBuilder().openApiVersion("3.1.0")
        builder.path("/account").operation("get") {
            responses {
                response("200") {
                    description("OK")
                    content {
                        mediaType("application/json") {
                            schema(schemaContext.inlineSchema(Account::class.java))
                        }
                    }
                }
            }
        }
        builder.resolveComponentReferences { type -> schemaContext.componentSchema(type) }
        return jsonMapper.readTree(builder.toJson())
    }

    private fun accountSchema(): JsonNode =
        accountDocument()
            .path("components")
            .path("schemas")
            .path("Account")

    private fun accountProperties(): JsonNode =
        accountSchema().path("properties")

    @Test
    fun `discovers component schemas reachable from an account response`() {
        val schemas = accountDocument().path("components").path("schemas")

        assertThat(schemas.fieldNames().asSequence().toList())
            .containsExactlyInAnyOrder("Account", "Address", "Role")
    }

    @Test
    fun `references the account component from a response`() {
        val document = accountDocument()

        assertThat(
            document.path("paths").path("/account").path("get")
                .path("responses").path("200").path("content")
                .path("application/json").path("schema").ref()
        ).isEqualTo("#/components/schemas/Account")
    }

    @Test
    fun `renders the account component with required properties`() {
        val account = accountSchema()
        val properties = account.path("properties")

        assertThat(account.path("type").asText()).isEqualTo("object")
        assertThat(account.path("required").stringArray()).containsExactlyInAnyOrder("id", "age")
        assertThat(properties.path("id").path("type").asText()).isEqualTo("string")
        assertThat(properties.path("age").path("type").asText()).isEqualTo("integer")
        assertThat(properties.path("age").path("format").asText()).isEqualTo("int32")
    }

    @Test
    fun `honors renamed account properties`() {
        val properties = accountProperties()

        assertThat(properties.path("e_mail").path("type").asText()).isEqualTo("string")
        assertThat(properties.has("email")).isFalse()
    }

    @Test
    fun `omits ignored account properties`() {
        val properties = accountProperties()

        assertThat(properties.has("secret")).isFalse()
    }

    @Test
    fun `renders account property descriptions`() {
        val properties = accountProperties()

        assertThat(properties.path("label").path("description").asText()).isEqualTo("Human readable label")
    }

    @Test
    fun `references object component properties`() {
        val properties = accountProperties()

        assertThat(properties.path("address").ref()).isEqualTo("#/components/schemas/Address")
    }

    @Test
    fun `references enum component properties`() {
        val properties = accountProperties()

        assertThat(properties.path("role").ref()).isEqualTo("#/components/schemas/Role")
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
    fun `renders object components`() {
        val schemas = accountDocument().path("components").path("schemas")

        val address = schemas.path("Address")
        assertThat(address.path("type").asText()).isEqualTo("object")
        assertThat(address.path("properties").fieldNames().asSequence().toList())
            .containsExactlyInAnyOrder("city", "zip")
    }

    @Test
    fun `renders enum components`() {
        val schemas = accountDocument().path("components").path("schemas")

        val role = schemas.path("Role")
        assertThat(role.path("type").asText()).isEqualTo("string")
        assertThat(role.path("enum").stringArray()).containsExactly("ADMIN", "regular_user")
    }
}
