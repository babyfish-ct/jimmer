package org.babyfish.jimmer.ksp.immutable.meta

import com.fasterxml.jackson.annotation.JsonFormat
import com.google.devtools.ksp.findActualType
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.Scalar
import org.babyfish.jimmer.dto.compiler.spi.BaseProp
import org.babyfish.jimmer.impl.util.Keywords
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.ksp.*
import org.babyfish.jimmer.ksp.immutable.generator.DRAFT
import org.babyfish.jimmer.ksp.immutable.generator.KEY_FULL_NAME
import org.babyfish.jimmer.ksp.immutable.generator.parseValidationMessages
import org.babyfish.jimmer.ksp.immutable.generator.upper
import org.babyfish.jimmer.ksp.util.ConverterMetadata
import org.babyfish.jimmer.ksp.util.converterMetadataOf
import org.babyfish.jimmer.ksp.util.fastResolve
import org.babyfish.jimmer.ksp.util.recursiveAnnotationOf
import org.babyfish.jimmer.meta.impl.PropDescriptor
import org.babyfish.jimmer.meta.impl.Utils
import org.babyfish.jimmer.sql.*
import java.util.regex.Pattern
import kotlin.reflect.KClass

class ImmutableProp(
    val ctx: Context,
    val declaringType: ImmutableType,
    val id: Int,
    val propDeclaration: KSPropertyDeclaration
): BaseProp {

    val resolvedType: KSType = propDeclaration.type.fastResolve()

    val typeAlias: KSTypeAlias? = resolvedType.declaration as? KSTypeAlias

    val realDeclaration: KSDeclaration = resolvedType.declaration.let {
        if (it is KSTypeAlias) {
            it.findActualType()
        } else {
            it
        }
    }

    init {
        if (propDeclaration.isMutable) {
            throw MetaException(
                propDeclaration,
                "the property of immutable interface must be readonly"
            )
        }
        if (propDeclaration.type is KSTypeAlias) {
            throw MetaException(
                propDeclaration,
                "the property of immutable interface cannot return type alias, please use real kotlin.type"
            )
        }
        if (propDeclaration.name.let { it.startsWith("is") && it.length > 2 && it[2].isUpperCase() } &&
            resolvedType.toTypeName().let { it != BOOLEAN && it != BOOLEAN.copy(nullable = true) }) {
            throw MetaException(
                propDeclaration,
                "the property whose name starts with \"is\" return returns boolean type"
            )
        }
        if (realDeclaration.modifiers.contains(Modifier.VALUE)) {
            throw MetaException(
                propDeclaration,
                "the property whose type is kotlin value class is not supported now"
            )
        }
        if (propDeclaration.annotation(LogicalDeleted::class) !== null) {
            val declaration = realDeclaration
            val typeName = if (declaration is KSClassDeclaration && declaration.classKind == ClassKind.ENUM_CLASS) {
                "<enum>"
            } else {
                declaration.fullName
            }
            when (typeName) {
                "kotlin.Boolean", "kotlin.Int", "<enum>" ->
                    if (resolvedType.isMarkedNullable) {
                        throw MetaException(
                            propDeclaration,
                            "the property decorated by \"@LogicalDeleted\" cannot be nullable " +
                                "if its type is boolean, int, or enum"
                        )
                    }
                "kotlin.Long", "java.util.UUID", "java.time.LocalDateTime", "java.time.Instant" -> {}
                else -> throw MetaException(
                    propDeclaration,
                    "the property decorated by \"@LogicalDeleted\" must be " +
                        "boolean, int, enum, UUID, LocalDateTime or Instant"
                )
            }
            if (propDeclaration.annotation(Default::class) !== null) {
                val isValid = when (typeName) {
                    "kotlin.Int", "<enum>" -> true
                    else -> false
                }
                if (!isValid) {
                    throw MetaException(
                        propDeclaration,
                        "the property cannot be decorated by both \"@Default\" and \"@LogicalDeleted\" " +
                            "unless its type is int or enum"
                    )
                }
            }
        }
    }

    override val name: String = propDeclaration.name.also {
        if (Keywords.ILLEGAL_PROP_NAMES.contains(it)) {
            throw MetaException(
                propDeclaration,
                "Illegal property \"$it\" which is jimmer keyword"
            )
        }
    }

    val slotName: String = "SLOT_${upper(name)}"

    override val isTransient: Boolean =
        annotation(Transient::class) !== null

    override fun hasTransientResolver(): Boolean =
        annotation(Transient::class)?.let {
            val resolverClassName = it.getClassArgument(Transient::value)?.toClassName()
            val resolverRef = it[Transient::ref] ?: ""
            val hasValue = resolverClassName != null && resolverClassName != UNIT
            val hasRef = resolverRef.isNotEmpty()
            if (hasValue && hasRef) {
                throw MetaException(
                    propDeclaration,
                    "it is decorated by @Transient, " +
                        "the `value` and `ref` are both specified, this is not allowed"
                )
            }
            hasValue || hasRef
        } ?: false

    override val isFormula: Boolean =
        annotation(Formula::class) !== null

    val isKotlinFormula: Boolean =
        annotation(Formula::class) !== null && !propDeclaration.isAbstract()

    override val isList: Boolean
        get() = if (isKotlinFormula || annotations { true }.any { isExplicitScalar(it, mutableSetOf()) }) {
            false
        } else {
            (if (resolvedType.declaration is KSClassDeclaration) {
                resolvedType.declaration as KSClassDeclaration
            } else {
                (resolvedType.declaration as KSTypeAlias).findActualType()
            }).asStarProjectedType().let { starType ->
                when {
                    isAssociation && ctx.mapType.isAssignableFrom(starType) ->
                        throw MetaException(propDeclaration, "it cannot be map")
                    ctx.collectionType.isAssignableFrom(starType) ->
                        if (!ctx.listType.isAssignableFrom(starType) ||
                            !resolvedType.isAssignableFrom(ctx.listType)
                        ) {
                            true
                        } else {
                            throw MetaException(propDeclaration, "collection property must be immutable list")
                        }
                    else -> false
                }
            }
        }

    override val isReference
        get() = !isList && isAssociation

    fun isDsl(isTableEx: Boolean): Boolean =
        when {
            idViewBaseProp != null -> false
            isKotlinFormula || isTransient || (idViewBaseProp !== null && idViewBaseProp!!.isList)-> false
            isRemote && isReverse -> false
            !isList && isRemote -> !isTableEx
            else -> true
        }

    private val targetDeclaration: KSClassDeclaration =
        if (isList) {
            val arguments = resolvedType.arguments.takeIf { it.isNotEmpty() }
                ?: throw MetaException(
                    propDeclaration,
                    "can extract the generic argument from property type"
                )
            arguments[0].type!!.fastResolve()
        } else {
            resolvedType
        }.declaration.also {
            if (it.annotation(MappedSuperclass::class) !== null) {
                throw MetaException(
                    propDeclaration,
                    "its target type \"$it\" is illegal, it cannot be type decorated by @MappedSuperclass"
                )
            }
        }.let {
            if (it is KSClassDeclaration) {
                it
            } else {
                (it as KSTypeAlias).findActualType()
            }
        }

    val primaryAnnotationType: Class<out Annotation>?

    private val _isNullable: Boolean

    override val isNullable: Boolean
        get() = _isNullable

    init {
        val descriptor = PropDescriptor
            .newBuilder(
                true,
                declaringType.toString(),
                declaringType.sqlAnnotationType?.java ?: Immutable::class.java,
                this.toString(),
                targetDeclaration.fullName,
                targetDeclaration.annotation(Entity::class)?.let { Entity::class.java }
                    ?: targetDeclaration.annotation(MappedSuperclass::class)?.let { MappedSuperclass::class.java }
                    ?: targetDeclaration.annotation(Embeddable::class)?.let { Embeddable::class.java }
                    ?: targetDeclaration.annotation(Immutable::class)?.let { Immutable::class.java },
                isList,
                resolvedType.isMarkedNullable
            ) {
                MetaException(propDeclaration, it)
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
        _isNullable = descriptor.isNullable
    }

    val isInputNotNull: Boolean =
        (annotation(ManyToOne::class) ?: annotation(OneToOne::class)) ?.let {
            val inputNotNull = it[ManyToOne::inputNotNull] ?: false
            if (inputNotNull && it[OneToMany::mappedBy]?.takeIf { v -> v.isNotEmpty() } !== null) {
                throw MetaException(
                    propDeclaration,
                    "the `inputNotNull` of annotation @${
                            it.annotationType.fastResolve().declaration.qualifiedName
                        } is true but the `mappedBy` of the annotation is specified " +
                        ""
                )
            }
            if (inputNotNull && !isNullable) {
                throw MetaException(
                    propDeclaration,
                    "the `inputNotNull` of annotation @${
                            it.annotationType.fastResolve().declaration.qualifiedName
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
            }?.also {
                if (declaringType.isAcrossMicroServices && targetDeclaration.annotation(Entity::class) != null && !isTransient) {
                    throw MetaException(
                        propDeclaration,
                        "association property is not allowed here " +
                            "because the declaring type is decorated by \"@" +
                            MappedSuperclass::class.java.name +
                            "\" with the argument `acrossMicroServices`"
                    )
                }
            } !== null

    override val isEmbedded: Boolean
        get() = targetType?.isEmbeddable ?: false

    override fun isAssociation(entityLevel: Boolean): Boolean =
        isAssociation && (!entityLevel || targetDeclaration.annotation(Entity::class) != null)

    val targetClassName: ClassName =
        targetDeclaration.nestedClassName()

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
            } else if (isNullable) {
                it.copy(nullable = true)
            } else {
                it
            }
        }

    val clientClassName: TypeName
        get() = converterMetadata?.targetTypeName?.copy(nullable = isNullable) ?: typeName()

    val targetType: ImmutableType? by lazy {
        targetDeclaration
            .takeIf { isAssociation }
            ?.let { ctx.typeOf(it) }
    }

    val isReferenceList = isAssociation && isList

    val isScalarList = isList && !isAssociation

    override val isId: Boolean =
        primaryAnnotationType == Id::class.java

    val isVersion: Boolean =
        primaryAnnotationType == Version::class.java

    override val isLogicalDeleted: Boolean =
        annotation(LogicalDeleted::class) !== null

    override val isExcludedFromAllScalars: Boolean =
        annotation(ExcludeFromAllScalars::class) !== null

    override val isKey: Boolean =
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

    val isRemote: Boolean by lazy {
        targetType?.takeIf {
            val remote = it.microServiceName != declaringType.microServiceName
            if (remote && annotation(JoinSql::class) !== null) {
                throw MetaException(
                    propDeclaration,
                    "remote association(micro-service names of declaring type and target type are different) " +
                        "cannot be decorated by \"@" +
                        JoinSql::class.qualifiedName +
                        "\""
                )
            }
            remote
        } !== null
    }

    val isReverse: Boolean =
        !(
            annotation(OneToOne::class)
                ?: annotation(OneToMany::class)
                ?: annotation(ManyToMany::class)
        )?.get(OneToOne::mappedBy).isNullOrEmpty()

    override val isRecursive: Boolean by lazy {
        declaringType.isEntity && manyToManyViewBaseProp === null && !isRemote &&
            declaringType.classDeclaration.asStarProjectedType().isAssignableFrom(
                targetDeclaration.asStarProjectedType()
            )
    }

    val valueFieldName: String?
        get() = if (idViewBaseProp === null && manyToManyViewBaseProp === null && !isKotlinFormula) {
            "__${name}Value"
        } else {
            null
        }

    val loadedFieldName: String? =
        if (idViewBaseProp === null && manyToManyViewBaseProp == null && !isKotlinFormula && (isNullable || isPrimitive)) {
            "__${name}Loaded"
        } else {
            null
        }

    val converterMetadata: ConverterMetadata? =
        run {
            val jsonConverter = propDeclaration.recursiveAnnotationOf(JsonConverter::class)
            val jsonFormat = propDeclaration.recursiveAnnotationOf(JsonFormat::class)
            if (jsonConverter !== null && jsonFormat !== null) {
                throw MetaException(
                    propDeclaration,
                    "it cannot be decorated both \"@${JsonConverter::class.qualifiedName}\" " +
                        "and \"${JsonFormat::class.qualifiedName}\""
                )
            }
            if (jsonConverter === null) {
                null
            } else {
                val declaration = jsonConverter.getClassArgument(JsonConverter::value)!!
                ctx.resolver.converterMetadataOf(declaration).also {
                    if (it.sourceTypeName != typeName(overrideNullable = false)) {
                        throw MetaException(
                            propDeclaration,
                            "the source type of converter " +
                                "\"${declaration.qualifiedName!!.asString()}\" is \"" +
                                "${it.sourceTypeName}\" does not match the return type of current property"
                        )
                    }
                }
            }
        }

    fun annotation(annotationType: KClass<out Annotation>): KSAnnotation? =
        propDeclaration.annotation(annotationType)

    fun annotations(annotationType: KClass<out Annotation>): List<KSAnnotation> =
        propDeclaration.annotations(annotationType)

    fun annotations(predicate: (KSAnnotation) -> Boolean): List<KSAnnotation> =
        propDeclaration.annotations(predicate)

    val getterAnnotations: List<KSAnnotation>
        get() = propDeclaration.getter?.annotations?.toList() ?: emptyList()

    val validationMessages: Map<ClassName, String> =
       parseValidationMessages(propDeclaration)

    override fun toString(): String =
        "${declaringType}.${propDeclaration.name}"

    private var _idViewBaseProp: ImmutableProp? = null

    private var _manyToManyViewBaseProp: ImmutableProp? = null

    private var _manyToManyViewBaseDeeperProp: ImmutableProp? = null

    private lateinit var _dependencies: Set<FormulaDependency>

    val idViewProp: ImmutableProp? by lazy {
        declaringType.properties.values.firstOrNull {
            it.idViewBaseProp == this
        }
    }

    val baseProp: ImmutableProp?
        get() = _idViewBaseProp ?: _manyToManyViewBaseProp

    override val idViewBaseProp: ImmutableProp?
        get() = _idViewBaseProp

    override val manyToManyViewBaseProp: ImmutableProp?
        get() = _manyToManyViewBaseProp

    val manyToManyViewBaseDeeperProp: ImmutableProp?
        get() = _manyToManyViewBaseDeeperProp

    val dependencies: Set<FormulaDependency>
        get() = _dependencies

    val isBaseProp: Boolean by lazy {
        for (otherProp in declaringType.properties.values) {
            for (dependency in otherProp.dependencies) {
                if (dependency.props.contains(this)) {
                    return@lazy true
                }
            }
            if (otherProp.idViewBaseProp == this) {
                return@lazy true
            }
            if (otherProp.manyToManyViewBaseDeeperProp == this) {
                return@lazy true
            }
        }
        false
    }

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

            3 -> {
                resolveManyToManyBaseViewProp()
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
        var base: String = idView[IdView::value] ?: ""
        if (base.isEmpty()) {
            base = Utils.defaultViewBasePropName(isList, name) ?: throw MetaException(
                    propDeclaration,
                    "it is decorated by \"@" +
                        IdView::class.java.name +
                        "\", the argument of that annotation is not specified by " +
                        "the base property name cannot be determined automatically, " +
                        "please specify the argument of that annotation"
                )
        }
        if (base == name) {
            throw MetaException(
                propDeclaration,
                "it is decorated by \"@" +
                    IdView::class.java.name +
                    "\", the argument of that annotation cannot be equal to the current property name\"" +
                    name +
                    "\""
            )
        }
        val baseProp = declaringType.properties[base]
            ?: throw MetaException(
                propDeclaration,
                "it is decorated by \"@" +
                    IdView::class.java.name +
                    "\" but there is no base property \"" +
                    base +
                    "\" in the declaring type"
            )
        if (!baseProp.isAssociation(true) || baseProp.isTransient) {
            throw MetaException(
                propDeclaration,
                "it is decorated by \"@" +
                    IdView::class.java.name +
                    "\" but the base property \"" +
                    baseProp +
                    "\" is not persistence association"
            )
        }
        if (isList != baseProp.isList) {
            throw MetaException(
                propDeclaration,
                "it " +
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
                propDeclaration,
                "it " +
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
                propDeclaration,
                "it is decorated by \"@" +
                    IdView::class.java.name +
                    "\", the base property \"" +
                    baseProp +
                    "\" returns entity type whose id is \"" +
                    targetIdTypeName +
                    "\", but the current property does not return that type"
            )
        }
        _idViewBaseProp = baseProp
    }

    private fun resolveManyToManyBaseViewProp() {
        val manyToManyView = annotation(ManyToManyView::class) ?: return
        val propName = manyToManyView[ManyToManyView::prop]!!
        val prop = declaringType.properties[propName]
            ?: throw MetaException(
                propDeclaration,
                "it is decorated by \"@" +
                    ManyToManyView::class.qualifiedName +
                    "\" with `prop` is \"" +
                    propName +
                    "\", but there is no such property in the declaring type"
            )
        if (prop.annotation(OneToMany::class) == null) {
            throw MetaException(
                propDeclaration,
                "it is decorated by \"@" +
                    ManyToManyView::class.qualifiedName +
                    "\" whose `prop` is \"" +
                    prop +
                    "\", but that property is not an one-to-many association"
            )
        }
        val middleType = prop.targetType!!
        val deeperPropName = manyToManyView[ManyToManyView::deeperProp] ?: ""
        val deeperProp = if (deeperPropName.isEmpty()) {
            var autoFoundProp: ImmutableProp? = null
            for (middleProp in middleType.properties.values) {
                if (middleProp.targetType === targetType &&
                    middleProp.annotation(ManyToOne::class) !== null) {
                    if (autoFoundProp !== null) {
                        throw MetaException(
                            propDeclaration,
                            "it is decorated by \"@" +
                                ManyToManyView::class.qualifiedName +
                                "\" whose `deeperProp` is not specified, " +
                                "however, two many-to-one properties pointing to target type are found: \"" +
                                autoFoundProp +
                                "\" and \"" +
                                prop +
                                "\", please specify its `deeperProp` explicitly"
                        );
                    }
                    autoFoundProp = prop;
                }
            }
            autoFoundProp
                ?: throw MetaException(
                    propDeclaration,
                    "it is decorated by \"@" +
                        ManyToManyView::class.qualifiedName +
                        "\" whose `deeperProp` is not specified, " +
                        "however, there is no many-to-one property pointing to " +
                        "target type in the middle entity type \"" +
                        middleType +
                        "\""
                )
        } else {
            middleType.properties[deeperPropName]
                ?.also {
                    if (it.targetType !== targetType || it.annotation(ManyToOne::class) === null) {
                        throw MetaException(
                            propDeclaration,
                            "it is decorated by \"@" +
                                ManyToManyView::class.qualifiedName +
                                "\" whose `deeperProp` is `" +
                                deeperPropName +
                                "`, " +
                                "however, there is no many-to-one property \"" +
                                deeperPropName +
                                "\" in the middle entity type \"" +
                                middleType +
                                "\""
                        )
                    }
                }
                ?: throw MetaException(
                    propDeclaration,
                    "it is decorated by \"@" +
                        ManyToManyView::class.qualifiedName +
                        "\" whose `deeperProp` is `" +
                        deeperPropName +
                        "`, " +
                        "however, there is no many-to-one property \"" +
                        deeperPropName +
                        "\" in the middle entity type \"" +
                        middleType +
                        "\""
                )
        }
        _manyToManyViewBaseProp = prop
        _manyToManyViewBaseDeeperProp = deeperProp
    }

    private fun resolveFormulaDependencies() {
        val dependencies = annotation(Formula::class)?.getListArgument(Formula::dependencies) ?: emptyList()
        if (dependencies.isEmpty()) {
            _dependencies = emptySet()
        } else {
            this._dependencies = dependencies.map { createFormulaDependency(this, it) }.toSet()
        }
    }

    companion object {

        fun isExplicitScalar(anno: KSAnnotation, handledQualifiedNames: MutableSet<String>): Boolean {
            if (!handledQualifiedNames.add(anno.fullName)) {
                return false
            }
            if (anno.fullName == Scalar::class.qualifiedName) {
                return true
            }
            for (deeperAnno in anno.annotationType.fastResolve().declaration.annotations { true }) {
                if (isExplicitScalar(deeperAnno, handledQualifiedNames)) {
                    return true
                }
            }
            return false
        }

        private val DOT_PATTERN = Pattern.compile("\\.")

        private fun createFormulaDependency(formulaProp: ImmutableProp, dependency: String): FormulaDependency {
            val propNames = DOT_PATTERN.split(dependency)
            val len = propNames.size
            var declaringType = formulaProp.declaringType
            val props = mutableListOf<ImmutableProp>()
            for (i in 0 until len) {
                val propName = propNames[i]
                val prop = declaringType.properties[propName]
                    ?: throw MetaException(
                        formulaProp.propDeclaration,
                        "The dependency \"" +
                            dependency +
                            "\" cannot be resolved because there is no property \"" +
                            propName +
                            "\" in \"" +
                            dependency +
                            "\""
                    )
                props += prop
                if (i + 1 < len) {
                    val targetType = prop.targetType
                    if (targetType === null) {
                        throw MetaException(
                            formulaProp.propDeclaration,
                            "The dependency \"" +
                                dependency +
                                "\" cannot be resolved because \"" +
                                prop +
                                "\" is not last property but it is neither association nor embedded property"
                        )
                    }
                    declaringType = targetType
                }
            }
            return FormulaDependency(props)
        }
    }
}
