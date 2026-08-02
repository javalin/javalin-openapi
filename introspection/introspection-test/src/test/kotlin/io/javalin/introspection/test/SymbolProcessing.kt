package io.javalin.introspection.test

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
import io.javalin.introspection.ksp.KspTypeIntrospector
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.reflect.KClass

@OptIn(ExperimentalCompilerApi::class)
object SymbolProcessing {

    fun <R> introspect(type: KClass<*>, block: (ClassDefinition) -> R): R {
        var result: Result<R>? = null
        val provider = object : SymbolProcessorProvider {
            override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
                object : SymbolProcessor {
                    override fun process(resolver: Resolver): List<KSAnnotated> {
                        if (result == null) {
                            result = runCatching { block(KspTypeIntrospector(resolver).introspect(type.qualifiedName!!)) }
                        }
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
}
