package io.javalin.introspection.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

@OptIn(ExperimentalCompilerApi::class)
class KspTypeIntrospectorTest {

    private fun <R> withKsp(block: (KspTypeIntrospector) -> R): R {
        var result: Result<R>? = null
        val provider = object : SymbolProcessorProvider {
            override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
                object : SymbolProcessor {
                    override fun process(resolver: Resolver): List<KSAnnotated> {
                        if (result == null) result = runCatching { block(KspTypeIntrospector(resolver)) }
                        return emptyList()
                    }
                }
        }
        val compilation = KotlinCompilation().apply {
            useKsp2()
            sources = listOf(SourceFile.kotlin("Trigger.kt", "package trigger\nclass Trigger"))
            symbolProcessorProviders = mutableListOf(provider)
            inheritClassPath = true
            messageOutputStream = System.out
        }
        val compiled = compilation.compile()
        check(compiled.exitCode == KotlinCompilation.ExitCode.OK) { "KSP compilation failed: ${compiled.messages}" }
        return (result ?: error("KSP processor did not run")).getOrThrow()
    }

    @Test
    fun `KSP backend resolves property types`() {
        val properties =
            withKsp { ksp ->
                ksp
                    .introspect("$REF_PKG.Account")
                    .getProperties()
                    .associate { property -> property.name to "${property.type.fullName}:${property.type.structureType}" }
            }

        assertThat(properties).containsAllEntriesOf(
            mapOf(
                "id" to "java.lang.String:DEFAULT",
                "age" to "java.lang.Integer:DEFAULT",
                "color" to "$REF_PKG.Color:DEFAULT",
                "address" to "$REF_PKG.Address:DEFAULT",
                "tags" to "java.lang.String:ARRAY",
                "meta" to "java.util.Map:DICTIONARY",
            )
        )
    }

    @Test
    fun `KSP backend resolves enum constants`() {
        val color =
            withKsp { ksp ->
                ksp.introspect("$REF_PKG.Color")
            }

        assertThat(color.isEnum()).isTrue()
        assertThat(color.getEnumConstants().map { it.name }).containsExactly("RED", "GREEN")
    }

    @Test
    fun `KSP backend reads annotations by name`() {
        val annotations =
            withKsp { ksp ->
                ksp
                    .introspect("$REF_PKG.Account")
                    .getAnnotations()
            }

        assertThat(annotations.contains("Ref")).isTrue()
        assertThat(annotations.find(Ref::class.java)?.get("value")?.asClassDefinition()?.fullName)
            .isEqualTo("$REF_PKG.Address")
    }

    @Test
    fun `KSP backend finds annotations by meta-annotation`() {
        val annotated =
            withKsp { ksp ->
                ksp
                    .introspect("$REF_PKG.Account")
                    .getAnnotations()
                    .all()
                    .first { it.metadata.contains("MetaMarker") }
            }

        assertThat(annotated["label"].asString()).isEqualTo("x")
    }

    private companion object {
        const val REF_PKG = "io.javalin.introspection.ksp"
    }
}

enum class Color { RED, GREEN }

annotation class Ref(val value: KClass<*>)

class Address(val city: String, val zip: String)

annotation class MetaMarker

@MetaMarker
annotation class Tagged(val label: String)

@Ref(Address::class)
@Tagged("x")
class Account(
    val id: String,
    val age: Int,
    val color: Color,
    val address: Address?,
    val tags: List<String>,
    val meta: Map<String, Int>,
)
