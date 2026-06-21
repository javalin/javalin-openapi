package io.javalin.openapi.dynamic

import com.fasterxml.jackson.databind.JsonNode
import io.javalin.openapi.dynamic.fixtures.Shape
import io.javalin.openapi.dynamic.fixtures.Validated
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DynamicCompositionTest {

    private val schemaContext = ReflectionSchemaContext()

    private fun propertiesOf(type: Class<*>): JsonNode =
        schemaContext.componentSchema(schemaContext.introspect(type)).json.path("properties")

    @Test
    fun `emits oneOf with refs for composition annotations`() {
        val refs = propertiesOf(Shape::class.java).path("animal").path("oneOf").map { it.path($$"$ref").asText() }
        assertThat(refs).containsExactlyInAnyOrder(
            "#/components/schemas/Dog",
            "#/components/schemas/Cat",
        )
    }

    @Test
    fun `omits an empty composition instead of emitting an invalid oneOf`() {
        assertThat(propertiesOf(Shape::class.java).path("empty").has("oneOf")).isFalse()
    }

    @Test
    fun `applies number validations`() {
        val score = propertiesOf(Validated::class.java).path("score")
        assertThat(score.path("minimum").asInt()).isEqualTo(1)
        assertThat(score.path("maximum").asInt()).isEqualTo(10)
    }

    @Test
    fun `applies the OpenApiPropertyType redirect`() {
        assertThat(propertiesOf(Validated::class.java).path("redirected").path("type").asText()).isEqualTo("string")
    }
}
