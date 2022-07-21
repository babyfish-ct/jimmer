package org.babyfish.jimmer.ksp.meta

import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.*
import org.babyfish.jimmer.ksp.generator.DRAFT
import org.babyfish.jimmer.ksp.generator.ID_FULL_NAME
import org.babyfish.jimmer.ksp.generator.KEY_FULL_NAME
import org.babyfish.jimmer.ksp.generator.VERSION_FULL_NAME
import org.babyfish.jimmer.sql.Key
import javax.persistence.*
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

    val primaryJpaAnnotation: KSAnnotation? =
        propDeclaration.let {
            val id = it.annotation(Id::class)
            val version = it.annotation(Version::class)
            val oneToOne = it.annotation(OneToOne::class)
            val manyToOne = it.annotation(ManyToOne::class)
            val oneToMany = it.annotation(OneToMany::class)
            val manyToMany = it.annotation(ManyToMany::class)
            val list = listOfNotNull(
                id,
                version,
                oneToOne,
                manyToOne,
                oneToMany,
                manyToMany
            )
            if (list.size > 1) {
                throw MetaException(
                    "Illegal property '${this}', conflict annotation " +
                        "'@${list[0].fullName}' and '@${list[1].fullName}'"
                )
            }
            if (id !== null) {
                if (resolvedType.isMarkedNullable) {
                    throw MetaException("Id property '${this}' cannot be nullable")
                }
                if (isAssociation) {
                    throw MetaException("Id property '${this}' cannot be association")
                }
            } else if (version !== null) {
                if (resolvedType != ctx.intType) {
                    throw MetaException("The type of version property '${propDeclaration}' must be int")
                }
            } else if (list.isNotEmpty()) {
                if (!isAssociation) {
                    throw MetaException(
                        "The property '${this}' cannot be declared by " +
                            "'@${list[0].fullName}' because it is not association"
                    )
                }
                if ((oneToOne !== null || manyToOne !== null) && !isReference) {
                    throw MetaException(
                        "The property '${this}' cannot be declared by " +
                            "'@${list[0].fullName}' because it is not reference association"
                    )
                }
                if ((oneToMany !== null || manyToMany !== null) && !isList) {
                    throw MetaException(
                        "The property '${this}' cannot be declared by " +
                            "'@${list[0].fullName}' because it is not list association"
                    )
                }
            }
            list.firstOrNull()
        }

    val isId: Boolean =
        primaryJpaAnnotation?.fullName == ID_FULL_NAME

    val isVersion: Boolean =
        primaryJpaAnnotation?.fullName == VERSION_FULL_NAME

    val isKey: Boolean =
        propDeclaration.annotations {
            it.fullName == KEY_FULL_NAME
        }.let { annotations ->
            if (annotations.isEmpty()) {
                false
            } else {
                if (isAssociation) {
                    if (primaryJpaAnnotation?.fullName != ManyToOne::class.qualifiedName ||
                        propDeclaration.annotations { it.fullName == JoinTable::class.qualifiedName}.isNotEmpty()) {
                        throw MetaException(
                            "Illegal property ${this}, when association property is " +
                                "decorated by '@org.babyfish.jimmer.sql.Key', " +
                                "it must be reference association based on foreign key"
                        )
                    }
                } else if (isNullable) {
                    throw MetaException(
                        "Illegal property ${this}, when scalar property is " +
                            "decorated by '@org.babyfish.jimmer.sql.Key', " +
                            "it must be non-null"
                    )
                }
                true
            }
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