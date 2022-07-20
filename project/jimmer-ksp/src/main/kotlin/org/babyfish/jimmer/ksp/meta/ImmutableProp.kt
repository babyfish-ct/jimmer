package org.babyfish.jimmer.ksp.meta

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.className
import org.babyfish.jimmer.ksp.fullName
import org.babyfish.jimmer.ksp.generator.DRAFT
import org.babyfish.jimmer.ksp.name
import javax.persistence.Id
import javax.persistence.Version

class ImmutableProp(
    val ctx: Context,
    val declaringType: ImmutableType,
    val id: Int,
    private val propDeclaration: KSPropertyDeclaration
) {
    init {
        if (!declaringType.isSqlType) {
            val sqlAnnotationName = propDeclaration.annotations.map {
                it.annotationType.resolve().declaration.fullName
            }.firstOrNull {
                it.startsWith("javax.persistence.")
            }
            if (sqlAnnotationName != null) {
                throw MetaException(
                    "'$propDeclaration' cannot be decorated by '@$sqlAnnotationName' " +
                    "because the current type is not sql type"
                )
            }
        }
    }

    val name: String = propDeclaration.name

    private val resolvedType: KSType = propDeclaration.type.resolve()

    val isList: Boolean =
        when {
            ctx.mapType.isAssignableFrom(resolvedType) ->
                throw MetaException("Illegal property '$propDeclaration', cannot be map")
            ctx.collectionType.isAssignableFrom(resolvedType) ->
                if (!ctx.listType.isAssignableFrom(resolvedType) ||
                    !resolvedType.isAssignableFrom(ctx.listType)) {
                    true
                } else {
                    throw MetaException("Illegal property '$propDeclaration', collection property must be immutable list")
                }
            else -> false
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
                if (draft) {
                    "$it$DRAFT"
                } else {
                    it
                }
            }

    fun typeName(draft: Boolean = false): TypeName =
        targetTypeName(draft)
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

    val isId: Boolean = if (propDeclaration.annotations.any { it.annotationType == Id::class }) {
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

    override fun toString(): String =
        propDeclaration.toString()
}