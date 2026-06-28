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

    @Test
    fun `renders a full component graph through OpenApiSchemaBuilder via reflection`() {
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

        val document = jsonMapper.readTree(builder.toJson())
        val schemas = document.path("components").path("schemas")

        // The whole ref graph was discovered and resolved to a fixpoint from a single root type.
        assertThat(schemas.fieldNames().asSequence().toList())
            .containsExactlyInAnyOrder("Account", "Address", "Role")

        // Root response is a $ref into the component graph.
        assertThat(
            document.path("paths").path("/account").path("get")
                .path("responses").path("200").path("content")
                .path("application/json").path("schema").ref()
        ).isEqualTo("#/components/schemas/Account")

        val account_ = schemas.path("Account")
        assertThat(account_.path("type").asText()).isEqualTo("object")
        assertThat(account_.path("required").stringArray()).containsExactlyInAnyOrder("id", "age")

        val properties = account_.path("properties")
        assertThat(properties.path("id").path("type").asText()).isEqualTo("string")
        assertThat(properties.path("age").path("type").asText()).isEqualTo("integer")
        assertThat(properties.path("age").path("format").asText()).isEqualTo("int32")
        assertThat(properties.path("e_mail").path("type").asText()).isEqualTo("string")
        assertThat(properties.has("email")).isFalse()
        assertThat(properties.has("secret")).isFalse()
        assertThat(properties.path("label").path("description").asText()).isEqualTo("Human readable label")

        // Nested object → $ref
        assertThat(properties.path("address").ref()).isEqualTo("#/components/schemas/Address")
        // Enum → $ref (resolved to a string enum component)
        assertThat(properties.path("role").ref()).isEqualTo("#/components/schemas/Role")

        // Collection → array of items
        assertThat(properties.path("tags").path("type").asText()).isEqualTo("array")
        assertThat(properties.path("tags").path("items").path("type").asText()).isEqualTo("string")

        // Map → object with additionalProperties
        assertThat(properties.path("meta").path("type").asText()).isEqualTo("object")
        assertThat(properties.path("meta").path("additionalProperties").path("type").asText()).isEqualTo("integer")

        val address = schemas.path("Address")
        assertThat(address.path("type").asText()).isEqualTo("object")
        assertThat(address.path("properties").fieldNames().asSequence().toList())
            .containsExactlyInAnyOrder("city", "zip")

        val role = schemas.path("Role")
        assertThat(role.path("type").asText()).isEqualTo("string")
        assertThat(role.path("enum").stringArray()).containsExactly("ADMIN", "regular_user")
    }
}
