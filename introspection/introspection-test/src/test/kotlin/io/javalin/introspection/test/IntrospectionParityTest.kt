package io.javalin.introspection.test

import io.javalin.introspection.Accessor
import io.javalin.introspection.ClassDefinition
import io.javalin.introspection.StructureType
import io.javalin.introspection.Visibility
import io.javalin.introspection.runtime.ReflectionTypeIntrospector
import io.javalin.introspection.test.fixtures.Account
import io.javalin.introspection.test.fixtures.Address
import io.javalin.introspection.test.fixtures.Annotated
import io.javalin.introspection.test.fixtures.Color
import io.javalin.introspection.test.fixtures.CrossPackageChild
import io.javalin.introspection.test.fixtures.Derived
import io.javalin.introspection.test.fixtures.Flagged
import io.javalin.introspection.test.fixtures.Flags
import io.javalin.introspection.test.fixtures.Holder
import io.javalin.introspection.test.fixtures.Outer
import io.javalin.introspection.test.fixtures.Point
import io.javalin.introspection.test.fixtures.Ref
import io.javalin.introspection.test.fixtures.Refs
import io.javalin.introspection.test.fixtures.Tricky
import io.javalin.introspection.test.fixtures.WithTransient
import io.javalin.introspection.test.fixtures.Wrapped
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class IntrospectionParityTest {

    private val runtime = ReflectionTypeIntrospector()

    private fun assertParity(type: KClass<*>) {
        val runtimeShape = runtime.introspect(type.java).toShape()
        val processedShape = AnnotationProcessing.introspect(type) { it.toShape() }
        assertThat(processedShape).isEqualTo(runtimeShape)
    }

    @Test fun `scalar, collection, map and nested members match across backends`() = assertParity(Account::class)

    @Test fun `plain object members match across backends`() = assertParity(Address::class)

    @Test fun `enum constants match across backends`() = assertParity(Color::class)

    @Test fun `inherited getters and private superclass fields match across backends`() = assertParity(Derived::class)

    @Test fun `record components match across backends`() = assertParity(Point::class)

    @Test fun `cross-package inherited fields match across backends`() = assertParity(CrossPackageChild::class)

    @Test
    fun `transient fields are flagged on both backends`() {
        assertParity(WithTransient::class)
        val transientByName = runtime.introspect(WithTransient::class.java).getProperties().associate { it.name to it.transient }
        assertThat(transientByName.getValue("skipped")).isTrue()
        assertThat(transientByName.getValue("kept")).isFalse()
    }

    @Test
    fun `getter detection excludes get-or-is lookalikes on both backends`() {
        assertParity(Tricky::class)
        val names = runtime.introspect(Tricky::class.java).getProperties().map { it.name }
        assertThat(names).contains("getName").doesNotContain("issue", "getaway", "getResult")
    }

    @Test
    fun `property-level annotations resolve identically across backends`() {
        val runtimeMarked = runtime.introspect(Annotated::class.java).getProperties().associate { it.name to it.annotations.hasNamed("Marker") }
        val processedMarked = AnnotationProcessing.introspect(Annotated::class) {
            it.getProperties().associate { property -> property.name to property.annotations.hasNamed("Marker") }
        }
        assertThat(processedMarked).isEqualTo(runtimeMarked)
        assertThat(runtimeMarked.getValue("getTagged")).isTrue()
        assertThat(runtimeMarked.getValue("getPlain")).isFalse()
    }

    @Test
    fun `nested annotation members normalize to maps identically across backends`() {
        val runtimeMeta = runtime.introspect(Wrapped::class.java).getAnnotations().memberValues(Outer::class.java)!!.getValue("meta")
        val processedMeta = AnnotationProcessing.introspect(Wrapped::class) { it.getAnnotations().memberValues(Outer::class.java)!!.getValue("meta") }
        assertThat(processedMeta).isEqualTo(runtimeMeta).isEqualTo(mapOf("note" to "x"))
    }

    @Test
    fun `Class-valued annotation members resolve identically across backends`() {
        val runtimeAnnotations = runtime.introspect(Holder::class.java).getAnnotations()
        val runtimeRef = runtimeAnnotations.resolveType(Ref::class.java) { value }?.fullName
        val runtimeRefs = runtimeAnnotations.resolveTypes(Refs::class.java) { value }.map { it.fullName }

        val (processedRef, processedRefs) = AnnotationProcessing.introspect(Holder::class) {
            val annotations = it.getAnnotations()
            annotations.resolveType(Ref::class.java) { value }?.fullName to annotations.resolveTypes(Refs::class.java) { value }.map { it.fullName }
        }

        assertThat(processedRef).isEqualTo(runtimeRef).isEqualTo(Address::class.java.name)
        assertThat(processedRefs).isEqualTo(runtimeRefs).containsExactly(Address::class.java.name, Color::class.java.name)
    }

    @Test
    fun `annotation value maps match across backends`() {
        val runtimeValue = runtime.introspect(Holder::class.java).getAnnotations().memberValues(Ref::class.java)!!.getValue("value")
        val processedValue = AnnotationProcessing.introspect(Holder::class) { it.getAnnotations().memberValues(Ref::class.java)!!.getValue("value") }
        assertThat((processedValue as ClassDefinition).fullName)
            .isEqualTo((runtimeValue as ClassDefinition).fullName)
            .isEqualTo(Address::class.java.name)
    }

    @Test
    fun `primitive array annotation members normalize identically across backends`() {
        val runtimeInts = runtime.introspect(Flagged::class.java).getAnnotations().memberValues(Flags::class.java)!!.getValue("ints")
        val processedInts = AnnotationProcessing.introspect(Flagged::class) { it.getAnnotations().memberValues(Flags::class.java)!!.getValue("ints") }
        assertThat(processedInts).isEqualTo(runtimeInts).isEqualTo(listOf(1, 2, 3))
    }
}

private data class TypeShape(
    val fullName: String,
    val simpleName: String,
    val structure: StructureType,
    val isEnum: Boolean,
    val enumConstants: List<String>?,
    val properties: List<PropertyShape>,
)

private data class PropertyShape(
    val name: String,
    val typeFullName: String,
    val typeStructure: StructureType,
    val typeGenerics: List<String>,
    val accessor: Accessor,
    val nullable: Boolean,
    val visibility: Visibility,
    val transient: Boolean,
)

/** Normalized, order-independent structural view of a type, so the two backends can be compared by value. */
private fun ClassDefinition.toShape(): TypeShape =
    TypeShape(
        fullName = fullName,
        simpleName = simpleName,
        structure = structureType,
        isEnum = isEnum(),
        enumConstants = getEnumConstants()?.sorted(),
        properties = if (isEnum()) emptyList() else getProperties()
            .map { PropertyShape(it.name, it.type.fullName, it.type.structureType, it.type.generics.map { g -> g.fullName }, it.accessor, it.nullable, it.visibility, it.transient) }
            .sortedBy { "${it.accessor}:${it.name}" },
    )
