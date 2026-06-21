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
import io.javalin.introspection.AnnotationView
import io.javalin.introspection.Annotations
import io.javalin.introspection.ClassDefinition
import io.javalin.introspection.EnumConstantView
import io.javalin.introspection.InternalIntrospectionApi
import io.javalin.introspection.PropertyView
import io.javalin.introspection.StructureType
import io.javalin.introspection.StructureType.ARRAY
import io.javalin.introspection.StructureType.DEFAULT
import io.javalin.introspection.StructureType.DICTIONARY
import io.javalin.introspection.TypeIntrospector
import io.javalin.introspection.Visibility
import com.google.devtools.ksp.symbol.Visibility as KspVisibility

// KSP can't materialize JVM annotation instances (annotation access is name/value only) and names builtins with
// Kotlin FQNs (`kotlin.Int`), which `canonicalName` normalizes to their JVM equivalents to match jap/reflection.
class KspTypeIntrospector(private val resolver: Resolver) : TypeIntrospector {

    private val mapType = builtin("kotlin.collections.Map")
    private val collectionType = builtin("kotlin.collections.Collection")

    override fun introspect(source: Any): ClassDefinition {
        require(source is KSType) { "KspTypeIntrospector expects a com.google.devtools.ksp.symbol.KSType, got ${source::class.java.name}" }
        return resolve(source)
    }

    fun introspect(qualifiedName: String): ClassDefinition {
        val declaration = resolver.getClassDeclarationByName(resolver.getKSNameFromString(qualifiedName))
            ?: error("KSP cannot resolve $qualifiedName")
        return resolve(declaration.asStarProjectedType())
    }

    fun annotationsOf(annotated: KSAnnotated): Annotations =
        KspAnnotations(annotated)

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

        @InternalIntrospectionApi
        override val source: Any
            get() = type

        override fun isEnum(): Boolean =
            declaration?.classKind == ClassKind.ENUM_CLASS

        override fun getEnumConstants(): List<EnumConstantView>? =
            declaration
                ?.takeIf { it.classKind == ClassKind.ENUM_CLASS }
                ?.declarations
                ?.filterIsInstance<KSClassDeclaration>()
                ?.filter { it.classKind == ClassKind.ENUM_ENTRY }
                ?.map { EnumConstantView(it.simpleName.asString(), KspAnnotations(it)) }
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
                    source = property,
                    annotations = KspAnnotations(property),
                )
            }.toList()
        }
    }

    private inner class KspAnnotations(private val element: KSAnnotated?) : Annotations {

        override fun hasNamed(simpleName: String): Boolean =
            element?.annotations?.any { it.shortName.asString() == simpleName } == true

        override fun memberValues(annotationType: Class<out Annotation>): Map<String, Any?>? =
            element?.annotations
                ?.firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationType.name }
                ?.let { argumentValues(it) }

        override fun memberValuesList(annotationType: Class<out Annotation>): List<Map<String, Any?>> =
            element?.annotations
                ?.filter { it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationType.name }
                ?.map { argumentValues(it) }
                ?.toList() ?: emptyList()

        override fun all(): List<AnnotationView> =
            element?.annotations?.map { KspAnnotationView(it) }?.toList() ?: emptyList()
    }

    private inner class KspAnnotationView(private val annotation: KSAnnotation) : AnnotationView {
        override val qualifiedName: String
            get() = annotation.annotationType.resolve().declaration.qualifiedName?.asString() ?: simpleName
        override val simpleName: String
            get() = annotation.shortName.asString()
        override val meta: Annotations
            get() = KspAnnotations(annotation.annotationType.resolve().declaration)
        override fun values(): Map<String, Any?> =
            argumentValues(annotation)
    }

    private fun argumentValues(annotation: KSAnnotation): Map<String, Any?> =
        annotation.arguments.associate { it.name!!.asString() to normalize(it.value) }

    private fun normalize(value: Any?): Any? =
        when (value) {
            is KSType -> resolve(value)
            is KSAnnotation -> argumentValues(value)
            is List<*> -> value.map { normalize(it) }
            is KSDeclaration -> value.simpleName.asString()
            else -> value
        }

    private fun visibilityOf(visibility: KspVisibility): Visibility =
        when (visibility) {
            KspVisibility.PUBLIC, KspVisibility.INTERNAL -> Visibility.PUBLIC
            KspVisibility.PROTECTED -> Visibility.PROTECTED
            KspVisibility.PRIVATE, KspVisibility.LOCAL -> Visibility.PRIVATE
            else -> Visibility.PACKAGE_PRIVATE
        }
}
