package org.babyfish.jimmer.ksp.meta

import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.*
import org.babyfish.jimmer.ksp.generator.DRAFT
import javax.persistence.Id
import javax.persistence.Version
import kotlin.reflect.KClass

class ImmutableProp(
    val ctx: Context,
    val declaringType: ImmutableType,
    val id: Int,
    private val propDeclaration: KSPropertyDeclaration
) {
    init {
        if (propDeclaration.isMutable) {
            throw MetaException("Illegal property '${this}', this property of immutable interface must be readonly")
        }
        if (!declaringType.isSqlType) {
            val sqlAnnotationNames = propDeclaration.annotations {
                it.annotationType.resolve().declaration.fullName.startsWith("javax.persistence.")
            }.map {
                it.annotationType.resolve().declaration.fullName
            }
            if (sqlAnnotationNames.isNotEmpty()) {
                throw MetaException(
                    "'$propDeclaration' cannot be decorated by $sqlAnnotationNames " +
                    "because the current type is not sql type"
                )
            }
        }
    }

    val name: String = propDeclaration.name

    private val resolvedType: KSType = propDeclaration.type.resolve()

    val isList: Boolean =
        (resolvedType.declaration as KSClassDeclaration).asStarProjectedType().let { starType ->
            when {
                ctx.mapType.isAssignableFrom(starType) ->
                    throw MetaException("Illegal property '$propDeclaration', cannot be map")
                ctx.collectionType.isAssignableFrom(starType) ->
                    if (!ctx.listType.isAssignableFrom(starType) ||
                        !resolvedType.isAssignableFrom(ctx.listType)) {
                        true
                    } else {
                        throw MetaException("Illegal property '$propDeclaration', collection property must be immutable list")
                    }
                else -> false
            }
        }

    val targetDeclaration: KSClassDeclaration =
        if (isList) {
            resolvedType.arguments[0].type!!.resolve()
        } else {
            resolvedType
        }.declaration as KSClassDeclaration

    val isAssociation: Boolean =
        targetDeclaration.classKind === ClassKind.INTERFACE &&
            ctx.typeAnnotationOf(targetDeclaration) != null

    val isNullable: Boolean =
        if (isList) {
            if (resolvedType.isMarkedNullable) {
                throw MetaException("Illegal property '${this}', list property cannot be null")
            }
            if (resolvedType.arguments[0].type!!.resolve().isMarkedNullable) {
                throw MetaException("Illegal property '${this}', elements of list property cannot be null")
            }
            false
        } else {
            resolvedType.isMarkedNullable
        }

    fun targetTypeName(
        draft: Boolean = false,
        overrideNullable: Boolean? = null
    ): ClassName =
        targetDeclaration
            .className(overrideNullable ?: isNullable) {
                if (draft && isAssociation) {
                    "$it$DRAFT"
                } else {
                    it
                }
            }

    fun typeName(draft: Boolean = false, overrideNullable: Boolean? = null): TypeName =
        targetTypeName(draft, overrideNullable)
            .let {
                if (isList) {
                    if (draft) {
                        MUTABLE_LIST.parameterizedBy(it)
                    } else {
                        LIST.parameterizedBy(it)
                    }
                } else {
                    it
                }
            }

    val targetType: ImmutableType? by lazy {
        targetDeclaration
            .takeIf { isAssociation }
            ?.let { ctx.typeOf(it) }
    }

    val isReference = isAssociation && !isList

    val isScalarList = isList && !isAssociation

    val isId: Boolean = if (propDeclaration.annotations{ it.annotationType == Id::class }.isNotEmpty()) {
        if (resolvedType.isMarkedNullable) {
            throw MetaException("Id property '${propDeclaration}' cannot be nullable")
        }
        if (isAssociation) {
            throw MetaException("Id property '${propDeclaration}' cannot be association")
        }
        true
    } else {
        false
    }

    val isVersion: Boolean = if (propDeclaration.annotations.any { it.annotationType == Version::class }) {
        if (resolvedType != ctx.intType) {
            throw MetaException("The type of version property '${propDeclaration}' must be int")
        }
        true
    } else {
        false
    }

    val isPrimitive: Boolean =
        if (!isList && !isNullable) {
            when (typeName()) {
                BOOLEAN, CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE -> true
                else -> false
            }
        } else {
            false
        }

    val valueFieldName: String = "__${name}Value"

    val loadedFieldName: String? =
        if (isNullable || isPrimitive) {
            "__${name}Loaded"
        } else {
            null
        }

    fun annotation(annotationType: KClass<out Annotation>): KSAnnotation? =
        propDeclaration.annotation(annotationType)

    fun annotations(annotationType: KClass<out Annotation>): List<KSAnnotation> =
        propDeclaration.annotations(annotationType)

    fun annotations(predicate: (KSAnnotation) -> Boolean): List<KSAnnotation> =
        propDeclaration.annotations(predicate)

    override fun toString(): String =
        "${declaringType}.${propDeclaration.name}"
}