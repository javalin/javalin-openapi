package io.javalin.introspection.test

import io.javalin.introspection.Accessor
import io.javalin.introspection.AnnotationSet
import io.javalin.introspection.ClassDefinition
import io.javalin.introspection.StructureType
import io.javalin.introspection.MemberVisibility
import io.javalin.introspection.runtime.ReflectionTypeIntrospector
import io.javalin.introspection.test.sub.PackagePrivateBase
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

    @Test
    fun `scalar, collection, map and nested members match across backends`() = assertParity(Account::class)

    @Test
    fun `plain object members match across backends`() = assertParity(Address::class)

    @Test
    fun `enum constants match across backends`() = assertParity(Color::class)

    @Test
    fun `inherited getters and private superclass fields match across backends`() = assertParity(Derived::class)

    @Test
    fun `record components match across backends`() = assertParity(Point::class)

    @Test
    fun `bounded type variables resolve to their bound across backends`() = assertParity(Bounded::class)

    @Test
    fun `cross-package inherited fields match across backends`() = assertParity(CrossPackageChild::class)

    @Test
    fun `transient fields are flagged on both backends`() {
        assertParity(WithTransient::class)
        val transientByName = runtime.introspect(WithTransient::class.java).getProperties()
            .filter { it.accessor == Accessor.FIELD }
            .associate { it.name to it.transient }
        assertThat(transientByName.getValue("skipped")).isTrue()
        assertThat(transientByName.getValue("kept")).isFalse()
    }

    @Test
    fun `getter detection excludes get-or-is lookalikes on both backends`() {
        assertParity(Tricky::class)
        val names = runtime.introspect(Tricky::class.java).getProperties().map { it.name }
        assertThat(names).contains("name").doesNotContain("issue", "getaway", "result")
    }

    @Test
    fun `property-level annotations resolve identically across backends`() {
        val runtimeMarked = runtime.introspect(Annotated::class.java).getProperties()
            .filter { it.accessor == Accessor.GETTER }
            .associate { it.name to it.annotations.contains("Marker") }
        val processedMarked = AnnotationProcessing.introspect(Annotated::class) {
            it.getProperties().filter { p -> p.accessor == Accessor.GETTER }
                .associate { p -> p.name to p.annotations.contains("Marker") }
        }
        assertThat(processedMarked).isEqualTo(runtimeMarked)
        assertThat(runtimeMarked.getValue("tagged")).isTrue()
        assertThat(runtimeMarked.getValue("plain")).isFalse()
    }

    @Test
    fun `annotation enumeration with meta-annotations matches across backends`() {
        fun scan(annotations: AnnotationSet): Pair<String, Any?> {
            val tagged = annotations.all().first { it.meta.contains("MetaMarker") }
            return tagged.simpleName to tagged.values["label"]
        }
        val runtimeScan = scan(runtime.introspect(Scanned::class.java).getAnnotations())
        val processedScan = AnnotationProcessing.introspect(Scanned::class) { scan(it.getAnnotations()) }
        assertThat(processedScan).isEqualTo(runtimeScan).isEqualTo("Tagged" to "x")
    }

    @Test
    fun `repeatable annotations enumerate identically across backends`() {
        fun notes(annotations: AnnotationSet) = annotations.findAll(Note::class.java).map { it.value("value") }
        val runtimeNotes = notes(runtime.introspect(Noted::class.java).getAnnotations())
        val processedNotes = AnnotationProcessing.introspect(Noted::class) { notes(it.getAnnotations()) }
        assertThat(processedNotes).isEqualTo(runtimeNotes)
        assertThat(runtimeNotes).containsExactlyInAnyOrder("a", "b")
    }

    @Test
    fun `nested annotation members normalize to maps identically across backends`() {
        val runtimeMeta = runtime.introspect(Wrapped::class.java).getAnnotations().find(Outer::class.java)!!.value("meta")
        val processedMeta = AnnotationProcessing.introspect(Wrapped::class) { it.getAnnotations().find(Outer::class.java)!!.value("meta") }
        assertThat(processedMeta).isEqualTo(runtimeMeta).isEqualTo(mapOf("note" to "x"))
    }

    @Test
    fun `Class-valued annotation members resolve identically across backends`() {
        val runtimeAnnotations = runtime.introspect(Holder::class.java).getAnnotations()
        val runtimeRef = runtimeAnnotations.find(Ref::class.java)?.classValue("value")?.fullName
        val runtimeRefs = runtimeAnnotations.find(Refs::class.java)?.classValues("value").orEmpty().map { it.fullName }

        val (processedRef, processedRefs) = AnnotationProcessing.introspect(Holder::class) {
            val annotations = it.getAnnotations()
            annotations.find(Ref::class.java)?.classValue("value")?.fullName to annotations.find(Refs::class.java)?.classValues("value").orEmpty().map { it.fullName }
        }

        assertThat(processedRef).isEqualTo(runtimeRef).isEqualTo(Address::class.java.name)
        assertThat(processedRefs).isEqualTo(runtimeRefs).containsExactly(Address::class.java.name, Color::class.java.name)
    }

    @Test
    fun `annotation value maps match across backends`() {
        val runtimeValue = runtime.introspect(Holder::class.java).getAnnotations().find(Ref::class.java)!!.value("value")
        val processedValue = AnnotationProcessing.introspect(Holder::class) { it.getAnnotations().find(Ref::class.java)!!.value("value") }
        assertThat((processedValue as ClassDefinition).fullName)
            .isEqualTo((runtimeValue as ClassDefinition).fullName)
            .isEqualTo(Address::class.java.name)
    }

    @Test
    fun `primitive array annotation members normalize identically across backends`() {
        val runtimeInts = runtime.introspect(Flagged::class.java).getAnnotations().find(Flags::class.java)!!.value("ints")
        val processedInts = AnnotationProcessing.introspect(Flagged::class) { it.getAnnotations().find(Flags::class.java)!!.value("ints") }
        assertThat(processedInts).isEqualTo(runtimeInts).isEqualTo(listOf(1, 2, 3))
    }

    @Test
    fun `KSP reports the same property names and types as reflection`() {
        fun namesAndTypes(definition: ClassDefinition) =
            definition.getProperties().map { it.name to it.type.fullName }.toSet()
        val runtimeProperties = namesAndTypes(runtime.introspect(Address::class.java))
        val kspProperties = SymbolProcessing.introspect(Address::class) { namesAndTypes(it) }
        assertThat(kspProperties).isEqualTo(runtimeProperties)
            .isEqualTo(setOf("city" to String::class.java.name, "zip" to String::class.java.name))
    }

    @Test
    fun `KSP collapses each property to a single getter where the JVM backends split field and getter`() {
        val kspAccessors = SymbolProcessing.introspect(Address::class) { definition ->
            definition.getProperties().map { it.accessor }
        }
        assertThat(kspAccessors).containsExactly(Accessor.GETTER, Accessor.GETTER)

        val runtimeAccessors = runtime.introspect(Address::class.java).getProperties().map { it.accessor }.toSet()
        assertThat(runtimeAccessors).containsExactlyInAnyOrder(Accessor.GETTER, Accessor.FIELD)
    }

    @Test
    fun `KSP enum constants match reflection`() {
        val runtimeConstants = runtime.introspect(Color::class.java).getEnumConstants().map { it.name }.sorted()
        val kspConstants = SymbolProcessing.introspect(Color::class) { it.getEnumConstants().map { constant -> constant.name }.sorted() }
        assertThat(kspConstants).isEqualTo(runtimeConstants).isEqualTo(listOf("GREEN", "RED"))
    }

    @Test
    fun `self-bounded type variables do not recurse forever across backends`() {
        fun childType(definition: ClassDefinition): Pair<String, List<String>> {
            val child = definition.getProperties().first { it.name == "child" }.type
            return child.fullName to child.generics.map { it.fullName }
        }

        val runtimeChild = childType(runtime.introspect(SelfBounded::class.java))
        val processedChild = AnnotationProcessing.introspect(SelfBounded::class) { childType(it) }
        val kspChild = SymbolProcessing.introspect(SelfBounded::class) { childType(it) }

        assertThat(processedChild).isEqualTo(runtimeChild)
        assertThat(kspChild.first).isEqualTo(SelfBounded::class.java.name)
        assertThat(kspChild.second).containsExactly(Any::class.java.name)
    }

    @Test
    fun `nested annotation classes can be found by class across backends`() {
        fun value(definition: ClassDefinition): String? =
            definition.getAnnotations().find(AnnotationContainer.Nested::class.java)?.string("value")

        val runtimeValue = value(runtime.introspect(NestedAnnotated::class.java))
        val processedValue = AnnotationProcessing.introspect(NestedAnnotated::class) { value(it) }
        val kspValue = SymbolProcessing.introspect(NestedAnnotated::class) { value(it) }

        assertThat(processedValue).isEqualTo(runtimeValue).isEqualTo("nested")
        assertThat(kspValue).isEqualTo(runtimeValue)
    }
}

private data class TypeShape(
    val fullName: String,
    val simpleName: String,
    val structure: StructureType,
    val isEnum: Boolean,
    val enumConstants: List<String>,
    val properties: List<PropertyShape>,
)

private data class PropertyShape(
    val name: String,
    val typeFullName: String,
    val typeStructure: StructureType,
    val typeGenerics: List<String>,
    val accessor: Accessor,
    val nullable: Boolean,
    val visibility: MemberVisibility,
    val transient: Boolean,
)

/** Normalized, order-independent structural view of a type, so the two backends can be compared by value. */
private fun ClassDefinition.toShape(): TypeShape =
    TypeShape(
        fullName = fullName,
        simpleName = simpleName,
        structure = structureType,
        isEnum = isEnum(),
        enumConstants = getEnumConstants().map { it.name }.sorted(),
        properties = if (isEnum()) emptyList() else getProperties()
            .map { PropertyShape(it.name, it.type.fullName, it.type.structureType, it.type.generics.map { g -> g.fullName }, it.accessor, it.nullable, it.visibility, it.transient) }
            .sortedBy { "${it.accessor}:${it.name}" },
    )

class Address(val city: String, val zip: String)

enum class Color { RED, GREEN }

class Box<T>(val value: T)

class Bounded<T : Address>(val value: T, val many: List<T>)

@JvmRecord
data class Point(val x: Int, val label: String, val tags: List<String>)

class Account(
    val id: String,
    val age: Int,
    val color: Color,
    val address: Address?,
    val tags: List<String>,
    val meta: Map<String, Int>,
    val bounded: Box<out Number>,
)

open class Base(val baseField: String) {
    val computed: String get() = ""
    protected val secret: String get() = ""
}

class Derived(val own: String) : Base("")

class CrossPackageChild : PackagePrivateBase()

class WithTransient(@Transient val skipped: String, val kept: String)

class Tricky {
    fun getName(): String = ""
    fun issue(): String = ""
    fun getaway(): String = ""
    fun getResult() {}
}

annotation class Marker

class Annotated(@get:Marker val tagged: String, val plain: String)

annotation class Meta(val note: String)

annotation class Outer(val meta: Meta)

@Outer(Meta("x"))
class Wrapped

annotation class MetaMarker

@MetaMarker
annotation class Tagged(val label: String)

@Tagged("x")
class Scanned

annotation class Flags(val ints: IntArray)

@Flags(ints = [1, 2, 3])
class Flagged

annotation class Ref(val value: KClass<*>)

annotation class Refs(vararg val value: KClass<*>)

@Ref(Address::class)
@Refs(Address::class, Color::class)
class Holder

class SelfBounded<T : SelfBounded<T>>(val child: T?)

class AnnotationContainer {
    annotation class Nested(val value: String)
}

@AnnotationContainer.Nested("nested")
class NestedAnnotated
