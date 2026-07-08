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
import io.javalin.introspection.ClassDefinition
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
    fun `KSP backend resolves structure, enums and name-based annotations`() {
        data class Snapshot(
            val properties: Map<String, String>,
            val isEnum: Boolean,
            val enumConstants: List<String>?,
            val hasRef: Boolean,
            val refValue: String?,
            val scannedLabel: Any?,
        )

        val snapshot = withKsp { ksp ->
            val pkg = "io.javalin.introspection.ksp"
            val account = ksp.introspect("$pkg.Account")
            val color = ksp.introspect("$pkg.Color")
            Snapshot(
                properties = account.getProperties().associate { it.name to "${it.type.fullName}:${it.type.structureType}" },
                isEnum = color.isEnum(),
                enumConstants = color.getEnumConstants().map { it.name },
                hasRef = account.getAnnotations().contains("Ref"),
                refValue = (account.getAnnotations().find(Ref::class.java)?.classValue("value"))?.fullName,
                scannedLabel = account.getAnnotations().all().first { it.meta.contains("MetaMarker") }.values["label"],
            )
        }

        // Structure + collection/map/nullability resolve, and Kotlin builtins normalize to JVM names (== jap/reflection)
        assertThat(snapshot.properties).containsAllEntriesOf(
            mapOf(
                "id" to "java.lang.String:DEFAULT",
                "age" to "java.lang.Integer:DEFAULT",
                "color" to "$REF_PKG.Color:DEFAULT",
                "address" to "$REF_PKG.Address:DEFAULT",
                "tags" to "java.lang.String:ARRAY",
                "meta" to "java.util.Map:DICTIONARY",
            )
        )

        // Enum
        assertThat(snapshot.isEnum).isTrue()
        assertThat(snapshot.enumConstants).containsExactly("RED", "GREEN")

        // Name-based annotation access (the only kind KSP can do): presence + class-valued member resolved to a ClassDefinition
        assertThat(snapshot.hasRef).isTrue()
        assertThat(snapshot.refValue).isEqualTo("$REF_PKG.Address")

        // Enumerate-all + meta-annotation scan (the custom-annotation path) works on KSP too
        assertThat(snapshot.scannedLabel).isEqualTo("x")
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
