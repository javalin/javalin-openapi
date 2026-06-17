package io.javalin.introspection.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Modifier
import io.javalin.introspection.Accessor
import io.javalin.introspection.Annotations
import io.javalin.introspection.ClassDefinition
import io.javalin.introspection.PropertyView
import io.javalin.introspection.StructureType
import io.javalin.introspection.StructureType.ARRAY
import io.javalin.introspection.StructureType.DEFAULT
import io.javalin.introspection.StructureType.DICTIONARY
import io.javalin.introspection.TypeIntrospector
import io.javalin.introspection.Visibility
import com.google.devtools.ksp.symbol.Visibility as KspVisibility

/**
 * Prototype [TypeIntrospector] backed by Kotlin Symbol Processing (KSP).
 *
 * KSP works on the *source/symbol* model, so it surfaces a Kotlin `val x` as one logical property (not a private
 * field + `getX()` like the compiled-bytecode backends), names builtins with Kotlin FQNs (`kotlin.Int`), and cannot
 * materialize JVM annotation instances — so annotation access here is purely name/value based.
 */
class KspTypeIntrospector(private val resolver: Resolver) : TypeIntrospector {

    private val mapType = builtin("kotlin.collections.Map")
    private val collectionType = builtin("kotlin.collections.Collection")

    override fun introspect(source: Any): ClassDefinition {
        require(source is KSType) { "KspTypeIntrospector expects a com.google.devtools.ksp.symbol.KSType, got ${source::class.java.name}" }
        return resolve(source)
    }

    /** Convenience for tests/harness: resolve a type by fully-qualified name. */
    fun introspect(qualifiedName: String): ClassDefinition {
        val declaration = resolver.getClassDeclarationByName(resolver.getKSNameFromString(qualifiedName))
            ?: error("KSP cannot resolve $qualifiedName")
        return resolve(declaration.asStarProjectedType())
    }

    private fun resolve(type: KSType, structureType: StructureType = DEFAULT): ClassDefinition {
        val declaration = type.declaration
        if (declaration is KSTypeParameter) {
            val bound = declaration.bounds.firstOrNull()?.resolve()
            return if (bound != null) resolve(bound, structureType) else objectDefinition(structureType)
        }
        val qualifiedName = declaration.qualifiedName?.asString() ?: return objectDefinition(structureType)
        return when {
            mapType != null && mapType.isAssignableFrom(type.starProjection()) ->
                definition(type, DICTIONARY, listOf(resolve(argument(type, 0)), resolve(argument(type, 1))))
            collectionType != null && collectionType.isAssignableFrom(type.starProjection()) ->
                resolve(argument(type, 0), ARRAY)
            qualifiedName == "kotlin.Array" ->
                resolve(argument(type, 0), ARRAY)
            else ->
                definition(type, structureType, type.arguments.mapNotNull { it.type?.resolve() }.map { resolve(it) })
        }
    }

    private fun definition(type: KSType, structureType: StructureType, generics: List<ClassDefinition>): ClassDefinition {
        val fullName = canonicalName(type.declaration)
        return Definition(
            simpleName = fullName.substringAfterLast('.'),
            fullName = fullName,
            generics = generics,
            structureType = structureType,
            type = type,
        )
    }

    /** Normalize Kotlin builtin FQNs (kotlin.Int, kotlin.String, kotlin.collections.Map, ...) to their JVM names, matching jap/reflection. */
    @OptIn(KspExperimental::class)
    private fun canonicalName(declaration: KSDeclaration): String {
        val kotlinName = declaration.qualifiedName ?: return declaration.simpleName.asString()
        return (resolver.mapKotlinNameToJava(kotlinName) ?: kotlinName).asString()
    }

    private fun objectDefinition(structureType: StructureType = DEFAULT): ClassDefinition =
        definition(builtin("kotlin.Any")!!, structureType, emptyList())

    private fun argument(type: KSType, index: Int): KSType =
        type.arguments.getOrNull(index)?.type?.resolve() ?: builtin("kotlin.Any")!!

    private fun builtin(qualifiedName: String): KSType? =
        resolver.getClassDeclarationByName(resolver.getKSNameFromString(qualifiedName))?.asStarProjectedType()

    private inner class Definition(
        simpleName: String,
        fullName: String,
        generics: List<ClassDefinition>,
        structureType: StructureType,
        private val type: KSType,
    ) : ClassDefinition(simpleName, fullName, generics, structureType) {

        private val declaration: KSClassDeclaration?
            get() = type.declaration as? KSClassDeclaration

        override val source: Any
            get() = type

        override fun isEnum(): Boolean =
            declaration?.classKind == ClassKind.ENUM_CLASS

        override fun getEnumConstants(): List<String>? =
            declaration
                ?.takeIf { it.classKind == ClassKind.ENUM_CLASS }
                ?.declarations
                ?.filterIsInstance<KSClassDeclaration>()
                ?.filter { it.classKind == ClassKind.ENUM_ENTRY }
                ?.map { it.simpleName.asString() }
                ?.toList()

        override fun getAnnotations(): Annotations =
            KspAnnotations(declaration)

        override fun getProperties(): List<PropertyView> {
            val declaration = declaration ?: return emptyList()
            return declaration.getAllProperties().map { property ->
                val propertyType = property.type.resolve()
                PropertyView(
                    name = property.simpleName.asString(),
                    type = resolve(propertyType),
                    accessor = Accessor.GETTER,
                    nullable = propertyType.isMarkedNullable,
                    visibility = visibilityOf(property.getVisibility()),
                    transient = Modifier.JAVA_TRANSIENT in property.modifiers,
                    annotations = KspAnnotations(property),
                )
            }.toList()
        }
    }

    private inner class KspAnnotations(private val element: KSAnnotated?) : Annotations {

        override fun hasNamed(simpleName: String): Boolean =
            element?.annotations?.any { it.shortName.asString() == simpleName } == true

        override fun memberValues(annotationType: Class<out Annotation>): Map<String, Any?>? {
            val annotation = annotationOf(annotationType.name) ?: return null
            return annotation.arguments.associate { it.name!!.asString() to normalize(it.value) }
        }

        private fun annotationOf(qualifiedName: String): KSAnnotation? =
            element?.annotations?.firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == qualifiedName }

        private fun normalize(value: Any?): Any? =
            when (value) {
                is KSType -> resolve(value)
                is KSAnnotation -> value.arguments.associate { it.name!!.asString() to normalize(it.value) }
                is List<*> -> value.map { normalize(it) }
                is KSDeclaration -> value.simpleName.asString()
                else -> value
            }
    }

    private fun visibilityOf(visibility: KspVisibility): Visibility =
        when (visibility) {
            KspVisibility.PUBLIC, KspVisibility.INTERNAL -> Visibility.PUBLIC
            KspVisibility.PROTECTED -> Visibility.PROTECTED
            KspVisibility.PRIVATE, KspVisibility.LOCAL -> Visibility.PRIVATE
            else -> Visibility.PACKAGE_PRIVATE
        }
}
