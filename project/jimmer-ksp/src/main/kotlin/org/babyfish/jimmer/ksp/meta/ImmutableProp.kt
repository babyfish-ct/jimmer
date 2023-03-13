package org.babyfish.jimmer.ksp.meta

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.ksp.*
import org.babyfish.jimmer.ksp.generator.DRAFT
import org.babyfish.jimmer.ksp.generator.KEY_FULL_NAME
import org.babyfish.jimmer.ksp.generator.parseValidationMessages
import org.babyfish.jimmer.meta.impl.PropDescriptor
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
    }

    val name: String = propDeclaration.name

    private val resolvedType: KSType = propDeclaration.type.resolve()

    val isTransient: Boolean =
        annotation(Transient::class) !== null

    fun hasTransientResolver(): Boolean =
        annotation(Transient::class)?.let {
            val resolverClassName = it.get<KSType>("value")?.toClassName()
            val resolverRef = it.get<String>("ref") ?: ""
            val hasValue = resolverClassName != null && resolverClassName != UNIT
            val hasRef = resolverRef.isNotEmpty()
            if (hasValue && hasRef) {
                throw MetaException(
                    "Illegal property \"" +
                        this +
                        "\", it is decorated by @Transient, " +
                        "the `value` and `ref` are both specified, this is not allowed"
                )
            }
            hasValue || hasRef
        } ?: false

    val isKotlinFormula: Boolean =
        annotation(Formula::class) != null && !propDeclaration.isAbstract()

    val isList: Boolean =
        (resolvedType.declaration as KSClassDeclaration).asStarProjectedType().let { starType ->
            when {
                isAssociation && ctx.mapType.isAssignableFrom(starType) ->
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

    fun isDsl(isTableEx: Boolean): Boolean =
        when {
            idViewBaseProp !== null || isKotlinFormula || isTransient -> false
            isList && isAssociation(true) -> isTableEx
            else -> true
        }

    private val targetDeclaration: KSClassDeclaration =
        if (isList) {
            resolvedType.arguments[0].type!!.resolve()
        } else {
            resolvedType
        }.declaration.also {
            if (it.annotation(MappedSuperclass::class) !== null) {
                throw MetaException(
                    "Illegal property \"$this\", its target type \"$it\" is illegal, it cannot be type decorated by @MappedSuperclass"
                )
            }
        } as KSClassDeclaration

    val primaryAnnotationType: Class<out Annotation>?

    val isNullable: Boolean

    init {
        val descriptor = PropDescriptor
            .newBuilder(
                declaringType.toString(),
                declaringType.sqlAnnotationType?.java,
                this.toString(),
                targetDeclaration.fullName,
                targetDeclaration.annotation(Entity::class)?.let { Entity::class.java }
                    ?: targetDeclaration.annotation(MappedSuperclass::class)?.let { MappedSuperclass::class.java }
                    ?: targetDeclaration.annotation(Embeddable::class)?.let { Embeddable::class.java }
                    ?: targetDeclaration.annotation(Immutable::class)?.let { Immutable::class.java },
                isList,
                resolvedType.isMarkedNullable
            ) {
                MetaException(it)
            }.apply {
                for (annotation in propDeclaration.annotations) {
                    add(annotation.fullName)
                    if (PropDescriptor.MAPPED_BY_PROVIDER_NAMES.contains(annotation.fullName) &&
                        annotation.arguments.any {
                            it.name?.asString() == "mappedBy" && it.value != ""
                        }) {
                        hasMappedBy()
                    }
                }
            }
            .build()
        primaryAnnotationType = descriptor.type.annotationType
        isNullable = descriptor.isNullable
    }

    val isInputNotNull: Boolean =
        (annotation(ManyToOne::class) ?: annotation(OneToOne::class)) ?.let {
            val inputNotNull = it["inputNotNull"] ?: false
            if (inputNotNull && it.get<String>("mappedBy")?.takeIf { v -> v.isNotEmpty() } !== null) {
                throw MetaException(
                    "Illegal property \"" +
                        this +
                        "\", the `inputNotNull` of annotation @${
                            it.annotationType.resolve().declaration.qualifiedName
                        } is true but the `mappedBy` of the annotation is specified " +
                        ""
                )
            }
            if (inputNotNull && !isNullable) {
                throw MetaException(
                    "Illegal property \"" +
                        this +
                        "\", the `inputNotNull` of annotation @${
                            it.annotationType.resolve().declaration.qualifiedName
                        } is true but the property is not nullable"
                )
            }
            inputNotNull
        } ?: false

    private val isAssociation: Boolean =
        (targetDeclaration.classKind === ClassKind.INTERFACE)
            ?.takeIf {
                it
            }
            ?.let {
                ctx.typeAnnotationOf(targetDeclaration)
            }
            ?.let {
                true
            } ?: false

    fun isAssociation(entityLevel: Boolean): Boolean =
        isAssociation && (!entityLevel || targetDeclaration.annotation(Entity::class) != null)

    val targetClassName: ClassName =
        targetDeclaration.className()

    fun targetTypeName(
        draft: Boolean = false,
        overrideNullable: Boolean? = null
    ): TypeName =
        if (isList) {
            (propDeclaration.type.toTypeName() as ParameterizedTypeName).typeArguments[0]
        } else {
            propDeclaration.type.toTypeName()
        }.let {
            if (draft && isAssociation && it is ClassName) {
                ClassName(it.packageName, "${it.simpleName}$DRAFT")
            } else {
                it
            }
        }.let {
            if (overrideNullable != null) {
                it.copy(nullable = overrideNullable)
            } else {
                it
            }
        }

    fun typeName(draft: Boolean = false, overrideNullable: Boolean? = null): TypeName =
        if (isList) {
            (propDeclaration.type.toTypeName() as ParameterizedTypeName).typeArguments[0]
        } else {
            propDeclaration.type.toTypeName()
        }.let {
            if (draft && isAssociation && it is ClassName) {
                ClassName(it.packageName, "${it.simpleName}$DRAFT")
            } else {
                it
            }
        }.let {
            when {
                isList && draft -> MUTABLE_LIST.parameterizedBy(it)
                isList -> LIST.parameterizedBy(it)
                else -> it
            }
        }.let {
            if (overrideNullable != null) {
                it.copy(nullable = overrideNullable)
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

    val isId: Boolean =
        primaryAnnotationType == Id::class.java

    val isVersion: Boolean =
        primaryAnnotationType == Version::class.java

    val isLogicalDeleted: Boolean =
        primaryAnnotationType == LogicalDeleted::class.java

    val isKey: Boolean =
        propDeclaration.annotations {
            it.fullName == KEY_FULL_NAME
        }.isNotEmpty()

    val isPrimitive: Boolean =
        if (!isList && !isNullable) {
            when (typeName()) {
                BOOLEAN, CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE -> true
                else -> false
            }
        } else {
            false
        }

    val valueFieldName: String?
        get() = if (isKotlinFormula || idViewBaseProp !== null) null else "__${name}Value"

    val loadedFieldName: String? =
        if (idViewBaseProp === null && !isKotlinFormula && (isNullable || isPrimitive)) {
            "__${name}Loaded"
        } else {
            null
        }

    val visibleFieldName: String?
        get() = if (_isVisibilityControllable) "__${name}Visible" else null

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

    private var _idViewBaseProp: ImmutableProp? = null

    private lateinit var _dependencies: Set<ImmutableProp>

    private var _isVisibilityControllable: Boolean = false

    val idViewBaseProp: ImmutableProp?
        get() = _idViewBaseProp

    val dependencies: Set<ImmutableProp>
        get() = _dependencies

    internal fun resolve(ctx: Context, step: Int): Boolean =
        when (step) {
            0 -> {
                resolveTargetType(ctx)
                true
            }
            1 -> {
                resolveIdViewBaseProp()
                true
            }
            2 -> {
                resolveFormulaDependencies()
                true
            }
            else -> false
        }

    private fun resolveTargetType(ctx: Context) {
        if (isAssociation) {
            targetType
        }
    }

    private fun resolveIdViewBaseProp() {
        val idView = annotation(IdView::class) ?: return
        var base: String = idView.get<String>("value") ?: ""
        if (base.isEmpty()) {
            if (!isList && name.length > 2 && !name[name.length - 3].isUpperCase() && name.endsWith("Id")) {
                base = name.substring(0, name.length - 2)
            } else {
                throw MetaException(
                    "Illegal property \"" +
                        this +
                        "\", it is decorated by \"@" +
                        IdView::class.java.name +
                        "\", the argument of that annotation is not specified by " +
                        "the base property name cannot be determined automatically, " +
                        "please specify the argument of that annotation"
                )
            }
        }
        if (base == name) {
            throw MetaException(
                "Illegal property \"" +
                    this +
                    "\", it is decorated by \"@" +
                    IdView::class.java.name +
                    "\", the argument of that annotation cannot be equal to the current property name\"" +
                    name +
                    "\""
            )
        }
        val baseProp = declaringType.properties[base]
            ?: throw MetaException(
                "Illegal property \"" +
                    this +
                    "\", it is decorated by \"@" +
                    IdView::class.java.name +
                    "\" but there is no base property \"" +
                    base +
                    "\" in the declaring type"
            )
        if (!baseProp.isAssociation(true) || baseProp.isTransient) {
            throw MetaException(
                "Illegal property \"" +
                    this +
                    "\", it is decorated by \"@" +
                    IdView::class.java.name +
                    "\" but the base property \"" +
                    baseProp +
                    "\" is not persistence association"
            )
        }
        if (isList != baseProp.isList) {
            throw MetaException(
                "Illegal property \"" +
                    this +
                    "\", it " +
                    (if (isList) "is" else "is not") +
                    " list and decorated by \"@" +
                    IdView::class.java.name +
                    "\" but the base property \"" +
                    baseProp +
                    "\" " +
                    (if (baseProp.isList) "is" else "is not") +
                    " list"
            )
        }
        if (isNullable != baseProp.isNullable) {
            throw MetaException(
                "Illegal property \"" +
                    this +
                    "\", it " +
                    (if (isNullable) "is" else "is not") +
                    " nullable and decorated by \"@" +
                    IdView::class.java.name +
                    "\" but the base property \"" +
                    baseProp +
                    "\" " +
                    (if (baseProp.isNullable) "is" else "is not") +
                    " nullable"
            )
        }
        val targetIdTypeName = baseProp.targetType!!.idProp!!.targetTypeName(
            overrideNullable = baseProp.isNullable
        )
        if (targetTypeName() != targetIdTypeName) {
            throw MetaException(
                "Illegal property \"" +
                    this +
                    "\", it is decorated by \"@" +
                    IdView::class.java.name +
                    "\", the base property \"" +
                    baseProp +
                    "\" returns entity type whose id is \"" +
                    targetIdTypeName +
                    "\", but the current property does not return that type"
            )
        }
        baseProp._isVisibilityControllable = true
        _isVisibilityControllable = true
        _idViewBaseProp = baseProp
    }

    private fun resolveFormulaDependencies() {
        val propNames = annotation(Formula::class)?.get<List<String>>("dependencies") ?: emptyList()
        if (propNames.isEmpty()) {
            _dependencies = emptySet()
        } else {
            val propMap = declaringType.properties
            val props = mutableSetOf<ImmutableProp>()
            for (dependency in propNames) {
                val prop = propMap[dependency]
                    ?: throw MetaException(
                        "Illegal property \"" +
                            this +
                            "\", it is decorated by \"@" +
                            Formula::class.qualifiedName +
                            "\" but the dependency property \"" +
                            dependency +
                            "\" does not eixst"
                    )
                props.add(prop)
                prop._isVisibilityControllable = true
            }
            this._isVisibilityControllable = true
            this._dependencies = props
        }
    }
}