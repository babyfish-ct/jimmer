package org.babyfish.jimmer.ksp.meta

import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.*
import org.babyfish.jimmer.ksp.generator.*
import org.babyfish.jimmer.ksp.generator.DRAFT
import org.babyfish.jimmer.ksp.generator.ID_FULL_NAME
import org.babyfish.jimmer.ksp.generator.KEY_FULL_NAME
import org.babyfish.jimmer.ksp.generator.VERSION_FULL_NAME
import org.babyfish.jimmer.meta.ModelException
import org.babyfish.jimmer.sql.*
import kotlin.reflect.KClass

class ImmutableProp(
    private val ctx: Context,
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
                it.annotationType.resolve().declaration.fullName.startsWith("org.babyfish.jimmer.sql.")
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

    val isTransient: Boolean =
        annotation(Transient::class) !== null

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
        }.declaration.also {
            if (it.annotation(MappedSuperclass::class) !== null) {
                throw ModelException(
                    "Illegal property \"$this\", its target type \"$it\" is illegal, it cannot be type decorated by @MappedSuperclass"
                )
            }
        } as KSClassDeclaration

    val isAssociation: Boolean =
        (targetDeclaration.classKind === ClassKind.INTERFACE)
            ?.takeIf {
                it
            }
            ?.let {
                ctx.typeAnnotationOf(targetDeclaration)
            }
            ?.let {
                if (declaringType.isEntity && it.fullName != Entity::class.qualifiedName && !isTransient) {
                    throw MetaException(
                        "Illegal property \"" +
                            this +
                            "\", association property of entity interface " +
                            "must reference to entity type or decorated by @Transient"
                    )
                }
                true
            }
            ?: false

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

    val primarySqlAnnotation: KSAnnotation? =
        propDeclaration.let {

            val id = it.annotation(Id::class)
            val version = it.annotation(Version::class)
            val oneToOne = it.annotation(OneToOne::class)
            val manyToOne = it.annotation(ManyToOne::class)
            val oneToMany = it.annotation(OneToMany::class)
            val manyToMany = it.annotation(ManyToMany::class)

            val annotations = listOfNotNull(
                id,
                version,
                oneToOne,
                manyToOne,
                oneToMany,
                manyToMany
            )
            if (annotations.size > 1) {
                throw MetaException(
                    "Illegal property '${this}', conflict annotation " +
                        "'@${annotations[0].fullName}' and '@${annotations[1].fullName}'"
                )
            }

            val column = it.annotation(Column::class)
            val joinColumn = it.annotation(JoinColumn::class)
            val joinTable = it.annotation(JoinTable::class)
            val storageAnnotations = listOfNotNull(
                column,
                joinColumn,
                joinTable
            )
            if (storageAnnotations.size > 1) {
                throw MetaException(
                    "Illegal property '${this}', conflict annotation " +
                        "'@${storageAnnotations[0].fullName}' and '@${storageAnnotations[1].fullName}'"
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
            } else if (annotations.isNotEmpty()) {
                if (!isAssociation) {
                    throw MetaException(
                        "The property '${this}' cannot be declared by " +
                            "'@${annotations[0].fullName}' because it is not association"
                    )
                }
                if ((oneToOne !== null || manyToOne !== null) && !isReference) {
                    throw MetaException(
                        "The property '${this}' cannot be declared by " +
                            "'@${annotations[0].fullName}' because it is not reference association"
                    )
                }
                if ((oneToMany !== null || manyToMany !== null) && !isList) {
                    throw MetaException(
                        "The property '${this}' cannot be declared by " +
                            "'@${annotations[0].fullName}' because it is not list association"
                    )
                }
            }

            val isMappedBy = annotations.isNotEmpty() &&
                annotations[0].arguments.any { arg ->
                    arg.name?.asString() == "mappedBy" &&
                        arg.value.let { v -> v is String && v.isNotEmpty() }
                }
            if (storageAnnotations.isNotEmpty()) {
                if (isMappedBy) {
                    throw MetaException(
                        "The property '${this}' cannot be decorated by " +
                            "'@${storageAnnotations[0].fullName}' " +
                            "because it is association property with 'mappedBy'"
                    )
                }
                if (column !== null && isAssociation) {
                    throw MetaException(
                        "The property '${this}' cannot be declared by " +
                            "'@${storageAnnotations[0].fullName}' because it is not association association"
                    )
                }
                if (joinColumn !== null && manyToOne === null && oneToOne === null) {
                    throw MetaException(
                        "The property '${this}' cannot be declared by " +
                            "'@${storageAnnotations[0].fullName}' because it is not many-to-one or one-to-one association"
                    )
                }
                if (joinTable !== null && manyToOne === null && manyToMany == null) {
                    throw MetaException(
                        "The property '${this}' cannot be declared by " +
                            "'@${storageAnnotations[0].fullName}' because " +
                            "it is neither many-to-one association nor many-to-many association"
                    )
                }
                manyToMany?.get<String>("mappedBy")?.takeIf { v -> v.isNotEmpty() }?.let {
                    throw MetaException(
                        "Cannot specify the 'mappedBy' for the property '${this}' " +
                            "because it is decorated by " +
                            "'@${storageAnnotations[0].fullName}'"
                    )
                }
            }

            if (oneToOne !== null && !resolvedType.isMarkedNullable) {
                throw MetaException("One-to-one property '${this}' must be nullable")
            }
            if (manyToOne !== null &&
                !resolvedType.isMarkedNullable &&
                joinTable != null
            ) {
                throw MetaException(
                    "Many-to-one property '${this}' " +
                        "is decorated by '${joinTable.fullName}', " +
                        "so it must be nullable"
                )
            }
            if (it.annotation(OnDissociate::class) != null) {
                if (isMappedBy) {
                    throw MetaException(
                        "The property '${this}' is illegal, " +
                            "The property with \"mappedBy\" can be decorated by @OnDissociate"
                    )
                }
                if (manyToOne == null && oneToOne == null) {
                    throw MetaException(
                        "The property '${this}' is illegal, " +
                            "only many-to-one or one-to-one property can be decorated by @OnDissociate"
                    )
                }
            }
            annotations.firstOrNull()
        }

    val isId: Boolean =
        primarySqlAnnotation?.fullName == ID_FULL_NAME

    val isVersion: Boolean =
        primarySqlAnnotation?.fullName == VERSION_FULL_NAME

    val isKey: Boolean =
        propDeclaration.annotations {
            it.fullName == KEY_FULL_NAME
        }.let { annotations ->
            if (annotations.isEmpty()) {
                false
            } else {
                if (isAssociation) {
                    if (primarySqlAnnotation?.fullName != ManyToOne::class.qualifiedName ||
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

    val validationMessages: Map<ClassName, String> =
       parseValidationMessages(propDeclaration)

    override fun toString(): String =
        "${declaringType}.${propDeclaration.name}"
}