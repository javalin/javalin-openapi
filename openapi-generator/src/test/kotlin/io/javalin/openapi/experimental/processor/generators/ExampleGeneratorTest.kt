package io.javalin.openapi.experimental.processor.generators

import io.javalin.openapi.experimental.processor.shared.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

internal class ExampleGeneratorTest {

    private fun generate(vararg examples: Map<String, Any?>): ExampleGenerator.GeneratorResult =
        ExampleGenerator.generateFromExamples(examples.map { it.toExampleProperty() })

    @Test
    fun `named examples preserve text raw JSON and nested objects`() {
        val result = generate(
            mapOf("name" to "text", "value" to "true"),
            mapOf("name" to "enabled", "raw" to "true"),
            mapOf("name" to "count", "raw" to "12"),
            mapOf("name" to "absent", "raw" to "null"),
            mapOf("name" to "nested", "objects" to listOf(mapOf("name" to "id", "value" to "42"))),
        )

        assertThat(result.simpleValue).isNull()
        assertThat(result.jsonElement).isEqualTo(jsonMapper.readTree(
            // language=json
            """{"text":"true","enabled":true,"count":12,"absent":null,"nested":{"id":"42"}}"""
        ))
    }

    @Test
    fun `unnamed text examples produce an array of strings`() {
        val result = generate(mapOf("value" to "1"), mapOf("value" to "true"))

        assertThat(result.jsonElement).isEqualTo(jsonMapper.readTree("""["1","true"]"""))
    }

    @Test
    fun `unnamed object examples produce an array of objects`() {
        val result = generate(
            mapOf("objects" to listOf(mapOf("name" to "id", "raw" to "1"))),
            mapOf("objects" to listOf(mapOf("name" to "id", "raw" to "2"))),
        )

        assertThat(result.jsonElement).isEqualTo(jsonMapper.readTree("""[{"id":1},{"id":2}]"""))
    }

    @Test
    fun `empty examples produce an empty object`() {
        assertThat(generate().jsonElement).isEqualTo(jsonMapper.createObjectNode())
    }

    @Test
    fun `object entries require names`() {
        assertThatThrownBy { generate(mapOf("raw" to "true")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("must have a name")
    }
}
