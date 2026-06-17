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
import io.javalin.introspection.ksp.fixtures.Ref
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
class KspTypeIntrospectorTest {

    /** Runs KSP in-process over a trivial trigger; the introspector resolves the classpath fixtures by name. */
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
        )

        val snapshot = withKsp { ksp ->
            val fixtures = "io.javalin.introspection.ksp.fixtures"
            val account = ksp.introspect("$fixtures.Account")
            val color = ksp.introspect("$fixtures.Color")
            Snapshot(
                properties = account.getProperties().associate { it.name to "${it.type.fullName}:${it.type.structureType}" },
                isEnum = color.isEnum(),
                enumConstants = color.getEnumConstants(),
                hasRef = account.getAnnotations().hasNamed("Ref"),
                refValue = (account.getAnnotations().memberValues(Ref::class.java)?.getValue("value") as? ClassDefinition)?.fullName,
            )
        }

        // Structure + collection/map/nullability resolve; NOTE KSP yields Kotlin FQNs (kotlin.String/Int), not java.lang.*
        assertThat(snapshot.properties.keys).containsExactlyInAnyOrder("id", "age", "color", "address", "tags", "meta")
        assertThat(snapshot.properties["tags"]).endsWith(":ARRAY")
        assertThat(snapshot.properties["meta"]).endsWith(":DICTIONARY")
        assertThat(snapshot.properties["color"]).isEqualTo("$REF_PKG.Color:DEFAULT")

        // Enum
        assertThat(snapshot.isEnum).isTrue()
        assertThat(snapshot.enumConstants).containsExactly("RED", "GREEN")

        // Name-based annotation access (the only kind KSP can do): presence + class-valued member resolved to a ClassDefinition
        assertThat(snapshot.hasRef).isTrue()
        assertThat(snapshot.refValue).isEqualTo("$REF_PKG.Address")
    }

    private companion object {
        const val REF_PKG = "io.javalin.introspection.ksp.fixtures"
    }
}
