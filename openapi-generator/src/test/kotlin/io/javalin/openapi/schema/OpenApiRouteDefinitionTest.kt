package io.javalin.openapi.schema

import io.javalin.introspection.AnnotationSet
import io.javalin.introspection.AnnotationProjection
import io.javalin.introspection.ClassDefinition
import io.javalin.introspection.EnumConstant
import io.javalin.introspection.InternalIntrospectionApi
import io.javalin.introspection.PropertyProjection
import io.javalin.openapi.experimental.EmbeddedTypeProcessor
import io.javalin.openapi.experimental.OpenApiType
import io.javalin.openapi.experimental.SchemaGenerationContext
import io.javalin.openapi.experimental.SimpleType
import io.javalin.openapi.experimental.StructureType
import io.javalin.openapi.experimental.processor.generators.TypeSchemaGenerator
import io.javalin.openapi.experimental.processor.shared.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class OpenApiRouteDefinitionTest {

    @Test
    fun `schemas resolved content properties without an unused fallback type`() {
        val source = StubClassDefinition()

        val json = OpenApiSchemaGenerator(TestSchemaContext(), "API", "1.0").generateSchema(
            listOf(
                mapOf(
                    "path" to "/owners",
                    "methods" to listOf("GET"),
                    "operationId" to "getOwners",
                    "responses" to listOf(
                        mapOf(
                            "status" to "200",
                            "content" to listOf(
                                mapOf(
                                    "mimeType" to "application/json",
                                    "properties" to listOf(
                                        mapOf(
                                            "from" to source,
                                            "name" to "owner",
                                            "isArray" to false,
                                        )
                                    ),
                                )
                            ),
                        )
                    ),
                )
            )
        )

        val document = jsonMapper.readTree(json)
        val ownerSchema = document
            .path("paths")
            .path("/owners")
            .path("get")
            .path("responses")
            .path("200")
            .path("content")
            .path("application/json")
            .path("schema")
            .path("properties")
            .path("owner")

        assertThat(ownerSchema.path("\$ref").asText())
            .isEqualTo("#/components/schemas/Owner")
    }

    private class StubClassDefinition : ClassDefinition(
        simpleName = "Owner",
        fullName = "example.Owner",
    ) {
        @OptIn(InternalIntrospectionApi::class)
        override val source: Any = Unit

        override fun isEnum(): Boolean = false
        override fun getEnumConstants(): List<EnumConstant> = emptyList()
        override fun getProperties(): List<PropertyProjection> = emptyList()
        override fun getAnnotations(): AnnotationSet = error("Not used by route definition decoding")
    }

    private class TestSchemaContext : SchemaGenerationContext {
        override val typeSchemaGenerator: TypeSchemaGenerator = TypeSchemaGenerator(this)
        override val simpleTypeMappings: Map<String, SimpleType> = emptyMap()
        override val embeddedTypeProcessors: List<EmbeddedTypeProcessor> = emptyList()

        override fun isEnum(type: OpenApiType): Boolean = false
        override fun annotationsOf(type: OpenApiType): AnnotationSet = EmptyAnnotations
        override fun propertiesOf(type: OpenApiType): List<PropertyProjection> = emptyList()
        override fun enumConstantsOf(type: OpenApiType): List<EnumConstant> = emptyList()

        override fun toOpenApiType(raw: ClassDefinition): OpenApiType =
            OpenApiType(
                simpleName = raw.simpleName,
                fullName = raw.fullName,
                generics = raw.generics.map(::toOpenApiType),
                structureType = StructureType.valueOf(raw.structureType.name),
            )
    }

    private object EmptyAnnotations : AnnotationSet {
        override fun all(): List<AnnotationProjection> = emptyList()
        override fun find(type: Class<out Annotation>): AnnotationProjection? = null
        override fun findAll(type: Class<out Annotation>): List<AnnotationProjection> = emptyList()
        override fun contains(simpleName: String): Boolean = false
    }
}
