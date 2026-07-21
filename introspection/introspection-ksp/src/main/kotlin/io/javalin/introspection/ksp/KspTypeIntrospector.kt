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
import io.javalin.introspection.AnnotationProjection
import io.javalin.introspection.AnnotationSet
import io.javalin.introspection.ClassDefinition
import io.javalin.introspection.CompileTimeIntrospector
import io.javalin.introspection.EnumConstant
import io.javalin.introspection.InternalIntrospectionApi
import io.javalin.introspection.PropertyProjection
import io.javalin.introspection.RepeatableAnnotationProjection
import io.javalin.introspection.StructureType
import io.javalin.introspection.StructureType.ARRAY
import io.javalin.introspection.StructureType.DEFAULT
import io.javalin.introspection.StructureType.DICTIONARY
import io.javalin.introspection.MemberVisibility
import java.lang.annotation.Repeatable as JavaRepeatable
import com.google.devtools.ksp.symbol.Visibility as KspVisibility

// KSP can't materialize JVM annotation instances (annotation access is name/value only) and names builtins with
// Kotlin FQNs (`kotlin.Int`), which `canonicalName` normalizes to their JVM equivalents to match jap/reflection.
class KspTypeIntrospector(private val resolver: Resolver) : CompileTimeIntrospector {

    private val mapType = builtin("kotlin.collections.Map")
    private val collectionType = builtin("kotlin.collections.Collection")
    private val primitiveArrayElementTypes = mapOf(
        "kotlin.BooleanArray" to "kotlin.Boolean",
        "kotlin.ByteArray" to "kotlin.Byte",
        "kotlin.ShortArray" to "kotlin.Short",
        "kotlin.IntArray" to "kotlin.Int",
        "kotlin.LongArray" to "kotlin.Long",
        "kotlin.FloatArray" to "kotlin.Float",
        "kotlin.DoubleArray" to "kotlin.Double",
        "kotlin.CharArray" to "kotlin.Char",
    )

    override fun introspect(source: Any): ClassDefinition {
        require(source is KSType) { "KspTypeIntrospector expects a com.google.devtools.ksp.symbol.KSType, got ${source::class.java.name}" }
        return resolve(source)
    }

    fun introspect(qualifiedName: String): ClassDefinition {
        val declaration = resolver.getClassDeclarationByName(resolver.getKSNameFromString(qualifiedName))
            ?: error("KSP cannot resolve $qualifiedName")
        return resolve(declaration.asStarProjectedType())
    }

    fun annotationsOf(annotated: KSAnnotated): AnnotationSet =
        KspAnnotations(annotated)

    @OptIn(InternalIntrospectionApi::class)
    override fun typesAnnotatedWith(annotationType: Class<out Annotation>, assignableTo: ClassDefinition?): List<ClassDefinition> {
        val target = assignableTo?.source as? KSType
        return resolver.getSymbolsWithAnnotation(annotationType.name)
            .filterIsInstance<KSClassDeclaration>()
            .map { it.asStarProjectedType() }
            .filter { target == null || target.isAssignableFrom(it) }
            .map { resolve(it) }
            .toList()
    }

    private fun resolve(
        type: KSType,
        structureType: StructureType = DEFAULT,
        visitingTypeParameters: Set<String> = emptySet(),
    ): ClassDefinition {
        val declaration = type.declaration
        if (declaration is KSTypeParameter) {
            val key = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()
            if (key in visitingTypeParameters) {
                return objectDefinition(structureType)
            }
            val bound = declaration.bounds.firstOrNull()?.resolve()
            return if (bound != null) {
                resolve(
                    type = bound,
                    structureType = structureType,
                    visitingTypeParameters = visitingTypeParameters + key,
                )
            } else {
                objectDefinition(structureType)
            }
        }
        val qualifiedName = declaration.qualifiedName?.asString() ?: return objectDefinition(structureType)
        return when {
            mapType != null && mapType.isAssignableFrom(type.starProjection()) -> {
                val keyType = resolve(argument(type, 0), visitingTypeParameters = visitingTypeParameters)
                val valueType = resolve(argument(type, 1), visitingTypeParameters = visitingTypeParameters)
                definition(
                    type = type,
                    structureType = DICTIONARY,
                    generics = listOf(keyType, valueType),
                )
            }
            collectionType != null && collectionType.isAssignableFrom(type.starProjection()) ->
                resolve(
                    type = argument(type, 0),
                    structureType = ARRAY,
                    visitingTypeParameters = visitingTypeParameters,
                )
            qualifiedName == "kotlin.Array" ->
                resolve(
                    type = argument(type, 0),
                    structureType = ARRAY,
                    visitingTypeParameters = visitingTypeParameters,
                )
            primitiveArrayElementTypes[qualifiedName] != null ->
                resolve(
                    type = builtin(primitiveArrayElementTypes.getValue(qualifiedName))!!,
                    structureType = ARRAY,
                    visitingTypeParameters = visitingTypeParameters,
                )
            else -> {
                val generics = type.arguments
                    .mapNotNull { it.type?.resolve() }
                    .map { resolve(it, visitingTypeParameters = visitingTypeParameters) }
                definition(type = type, structureType = structureType, generics = generics)
            }
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
        definition(
            type = builtin("kotlin.Any")!!,
            structureType = structureType,
            generics = emptyList(),
        )

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
    ) : ClassDefinition(
        simpleName = simpleName,
        fullName = fullName,
        generics = generics,
        structureType = structureType,
    ) {

        private val declaration: KSClassDeclaration?
            get() = type.declaration as? KSClassDeclaration

        @InternalIntrospectionApi
        override val source: Any
            get() = type

        override fun isEnum(): Boolean =
            declaration?.classKind == ClassKind.ENUM_CLASS

        override fun getEnumConstants(): List<EnumConstant> =
            declaration
                ?.takeIf { it.classKind == ClassKind.ENUM_CLASS }
                ?.declarations
                ?.filterIsInstance<KSClassDeclaration>()
                ?.filter { it.classKind == ClassKind.ENUM_ENTRY }
                ?.map {
                    EnumConstant(
                        name = it.simpleName.asString(),
                        annotations = KspAnnotations(elements = listOf(it)),
                    )
                }
                ?.toList()
                .orEmpty()

        override fun getAnnotations(): AnnotationSet = KspAnnotations(declaration)

        override fun getProperties(): List<PropertyProjection> {
            val declaration = declaration ?: return emptyList()
            return declaration
                .getAllProperties()
                .filter { property -> property.getVisibility() !in setOf(KspVisibility.PRIVATE, KspVisibility.LOCAL) }
                .map { property ->
                    val propertyType = property.type.resolve()
                    PropertyProjection(
                        name = property.simpleName.asString(),
                        type = resolve(propertyType),
                        accessor = Accessor.GETTER,
                        nullable = propertyType.isMarkedNullable,
                        visibility = visibilityOf(property.getVisibility()),
                        transient = Modifier.JAVA_TRANSIENT in property.modifiers,
                        source = property,
                        annotations = KspAnnotations(elements = listOfNotNull(property, property.getter)),
                    )
                }
                .toList()
        }
    }

    private inner class KspAnnotations(private val elements: List<KSAnnotated>) : AnnotationSet {

        constructor(element: KSAnnotated?) : this(listOfNotNull(element))

        private fun annotations(): List<KSAnnotation> =
            elements.flatMap { it.annotations.toList() }

        override fun contains(simpleName: String): Boolean =
            annotations().any { it.shortName.asString() == simpleName }

        override fun find(type: Class<out Annotation>): AnnotationProjection? =
            annotations()
                .firstOrNull { it.named(type) }
                ?.let { KspAnnotationProjection(it) }

        override fun findAll(type: Class<out Annotation>): List<AnnotationProjection> {
            val annotations = annotations()
            val direct = annotations
                .filter { it.named(type) }
                .map { KspAnnotationProjection(it) }

            val containerName = type.getAnnotation(JavaRepeatable::class.java)?.value?.java?.canonicalName
            val repeated = containerName
                ?.let { name -> annotations.firstOrNull { it.qualifiedName() == name } }
                ?.let { argumentValues(it)["value"] as? List<*> }
                ?.filterIsInstance<Map<String, Any?>>()
                ?.map { RepeatableAnnotationProjection(type.simpleName, it) }
                .orEmpty()
            return direct + repeated
        }

        override fun all(): List<AnnotationProjection> =
            annotations().distinctBy { it.annotationType.resolve().declaration }.map { KspAnnotationProjection(it) }

        private fun KSAnnotation.named(type: Class<out Annotation>): Boolean {
            val qualifiedName = qualifiedName()
            return qualifiedName == type.name || qualifiedName == type.canonicalName
        }

        private fun KSAnnotation.qualifiedName(): String? =
            annotationType.resolve().declaration.qualifiedName?.asString()
    }

    private inner class KspAnnotationProjection(private val annotation: KSAnnotation) : AnnotationProjection {

        override val simpleName: String
            get() = annotation.shortName.asString()

        override val metadata: AnnotationSet
            get() = KspAnnotations(annotation.annotationType.resolve().declaration)

        override val values: Map<String, Any?>
            get() = argumentValues(annotation)

    }

    private fun argumentValues(annotation: KSAnnotation): Map<String, Any?> =
        annotation.arguments.mapNotNull { argument ->
            argument.name?.asString()?.let { it to normalize(argument.value) }
        }.toMap()

    private fun normalize(value: Any?): Any? =
        when (value) {
            is KSType -> resolve(value)
            is KSAnnotation -> argumentValues(value)
            is List<*> -> value.map { normalize(it) }
            is KSDeclaration -> value.simpleName.asString()
            else -> value
        }

    private fun visibilityOf(visibility: KspVisibility): MemberVisibility =
        when (visibility) {
            KspVisibility.PUBLIC, KspVisibility.INTERNAL -> MemberVisibility.PUBLIC
            KspVisibility.PROTECTED -> MemberVisibility.PROTECTED
            KspVisibility.PRIVATE, KspVisibility.LOCAL -> MemberVisibility.PRIVATE
            else -> MemberVisibility.PACKAGE_PRIVATE
        }
}
