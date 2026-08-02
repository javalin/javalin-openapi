package io.javalin.openapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class OpenApiStatusTest {

    @Test
    fun `returns known unknown and non-numeric status phrases`() {
        assertThat(OpenApiStatus.reasonPhrase(OpenApiStatus.OK)).isEqualTo("OK")
        assertThat(OpenApiStatus.reasonPhrase("599")).isEqualTo("Unknown HTTP code")
        assertThat(OpenApiStatus.reasonPhrase("default")).isNull()
    }
}
