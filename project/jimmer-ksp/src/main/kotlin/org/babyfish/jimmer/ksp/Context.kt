package org.babyfish.jimmer.ksp

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Origin
import com.squareup.kotlinpoet.ClassName
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.dto.compiler.SourceTypeFilter
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass

class Context(
    val resolver: Resolver,
    val environment: SymbolProcessorEnvironment
) {
    val collectionType: KSType = resolver
        .getClassDeclarationByName("kotlin.collections.Collection")
        ?.asStarProjectedType()
        ?: error("Internal bug")

    val listType: KSType = resolver
        .getClassDeclarationByName("kotlin.collections.List")
        ?.asStarProjectedType()
        ?: error("Internal bug")

    val mapType: KSType = resolver
        .getClassDeclarationByName("kotlin.collections.Map")
        ?.asStarProjectedType()
        ?: error("Internal bug")

    val isHibernateValidatorEnhancement: Boolean =
        environment.options["jimmer.dto.hibernateValidatorEnhancement"] == "true"

    val isBuddyIgnoreResourceGeneration: Boolean =
        environment.options["jimmer.buddy.ignoreResourceGeneration"]?.trim() == "true"

    val jackson3 = detectIsJackson3(resolver, environment)

    val jacksonTypes: JacksonTypes =
        if (jackson3) {
            JacksonTypes(
                jsonIgnore = ClassName("com.fasterxml.jackson.annotation", "JsonIgnore"),
                jsonValue = ClassName("com.fasterxml.jackson.annotation", "JsonValue"),
                jsonTypeInfo = ClassName("com.fasterxml.jackson.annotation", "JsonTypeInfo"),
                jsonSubTypes = ClassName("com.fasterxml.jackson.annotation", "JsonSubTypes"),
                jsonTypeName = ClassName("com.fasterxml.jackson.annotation", "JsonTypeName"),
                jsonFormat = ClassName("com.fasterxml.jackson.annotation", "JsonFormat"),
                jsonProperty = ClassName("com.fasterxml.jackson.annotation", "JsonProperty"),
                jsonPropertyOrder = ClassName("com.fasterxml.jackson.annotation", "JsonPropertyOrder"),
                jsonCreator = ClassName("com.fasterxml.jackson.annotation", "JsonCreator"),
                jsonSerializer = ClassName("tools.jackson.databind", "ValueSerializer"),
                jsonSerialize = ClassName("tools.jackson.databind.annotation", "JsonSerialize"),
                jsonDeserialize = ClassName("tools.jackson.databind.annotation", "JsonDeserialize"),
                jsonPojoBuilder = ClassName("tools.jackson.databind.annotation", "JsonPOJOBuilder"),
                jsonNaming = ClassName("tools.jackson.databind.annotation", "JsonNaming"),
                jsonGenerator = ClassName("tools.jackson.core", "JsonGenerator"),
                serializeProvider = ClassName("tools.jackson.databind", "SerializationContext")
            )
        } else {
            JacksonTypes(
                jsonIgnore = ClassName("com.fasterxml.jackson.annotation", "JsonIgnore"),
                jsonValue = ClassName("com.fasterxml.jackson.annotation", "JsonValue"),
                jsonTypeInfo = ClassName("com.fasterxml.jackson.annotation", "JsonTypeInfo"),
                jsonSubTypes = ClassName("com.fasterxml.jackson.annotation", "JsonSubTypes"),
                jsonTypeName = ClassName("com.fasterxml.jackson.annotation", "JsonTypeName"),
                jsonFormat = ClassName("com.fasterxml.jackson.annotation", "JsonFormat"),
                jsonProperty = ClassName("com.fasterxml.jackson.annotation", "JsonProperty"),
                jsonPropertyOrder = ClassName("com.fasterxml.jackson.annotation", "JsonPropertyOrder"),
                jsonCreator = ClassName("com.fasterxml.jackson.annotation", "JsonCreator"),
                jsonSerializer = ClassName("com.fasterxml.jackson.databind", "JsonSerializer"),
                jsonSerialize = ClassName("com.fasterxml.jackson.databind.annotation", "JsonSerialize"),
                jsonDeserialize = ClassName("com.fasterxml.jackson.databind.annotation", "JsonDeserialize"),
                jsonPojoBuilder = ClassName("com.fasterxml.jackson.databind.annotation", "JsonPOJOBuilder"),
                jsonNaming = ClassName("com.fasterxml.jackson.databind.annotation", "JsonNaming"),
                jsonGenerator = ClassName("com.fasterxml.jackson.core", "JsonGenerator"),
                serializeProvider = ClassName("com.fasterxml.jackson.databind", "SerializerProvider")
            )
        }

    private val sourceTypeFilter = SourceTypeFilter(
        environment.options["jimmer.source.includes"],
        environment.options["jimmer.source.excludes"]
    )

    private val typeMap: MutableMap<KSClassDeclaration, ImmutableType> = mutableMapOf()

    private var newTypes = typeMap?.values?.toMutableList() ?: mutableListOf()

    val types: Collection<ImmutableType>
        get() = typeMap.values

    fun typeOf(classDeclaration: KSClassDeclaration): ImmutableType =
        typeMap[classDeclaration] ?: ImmutableType(this, classDeclaration).also {
            typeMap[classDeclaration] = it
            newTypes += it
        }

    fun typeAnnotationOf(classDeclaration: KSClassDeclaration): KSAnnotation? {
        var sqlAnnotation: KSAnnotation? = null
        for (ormAnnotationType in ORM_ANNOTATION_TYPES) {
            val anno = classDeclaration.annotation(ormAnnotationType) ?: continue
            if (sqlAnnotation !== null) {
                throw MetaException(
                    classDeclaration,
                    null,
                    "it cannot be decorated by both " +
                            "@${sqlAnnotation.fullName} and ${anno.fullName}"
                )
            }
            sqlAnnotation = anno
        }
        return sqlAnnotation ?: classDeclaration.annotation(Immutable::class)
    }

    fun resolve() {
        while (this.newTypes.isNotEmpty()) {
            val newTypes = this.newTypes
            this.newTypes = mutableListOf()
            for (newType in newTypes) {
                for (step in 0..4) {
                    newType.resolve(this, step)
                }
            }
        }
    }

    fun include(declaration: KSClassDeclaration): Boolean {
        val qualifiedName = declaration.qualifiedName!!.asString()
        return isKotlinType(declaration) && sourceTypeFilter.test(qualifiedName)
    }

    fun includeDtoTarget(qualifiedName: String): Boolean {
        if (!sourceTypeFilter.test(qualifiedName)) {
            return false
        }
        val declaration = resolver.getClassDeclarationByName(qualifiedName)
        return declaration === null || isKotlinType(declaration)
    }

    private fun isKotlinType(declaration: KSClassDeclaration): Boolean =
        declaration.origin == Origin.KOTLIN || declaration.origin == Origin.KOTLIN_LIB

    companion object {
        private val ORM_ANNOTATION_TYPES = listOf(
            Entity::class,
            MappedSuperclass::class,
            Embeddable::class
        )

        private fun detectIsJackson3(resolver: Resolver, environment: SymbolProcessorEnvironment): Boolean {
            val jackson3Text = environment.options["jimmer.jackson3"]
            return if (jackson3Text.isNullOrEmpty()) {
                resolver.getClassDeclarationByName("tools.jackson.databind.ObjectMapper") != null
            } else {
                "true" == jackson3Text
            }
        }
    }
}
