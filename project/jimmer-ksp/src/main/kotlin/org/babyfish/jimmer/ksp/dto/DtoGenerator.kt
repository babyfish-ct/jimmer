package org.babyfish.jimmer.ksp.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BOOLEAN_ARRAY
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.CHAR_ARRAY
import com.squareup.kotlinpoet.COLLECTION
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.DOUBLE_ARRAY
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FLOAT_ARRAY
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.INT_ARRAY
import com.squareup.kotlinpoet.ITERABLE
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.LONG_ARRAY
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MUTABLE_COLLECTION
import com.squareup.kotlinpoet.MUTABLE_ITERABLE
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.SHORT_ARRAY
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import org.babyfish.jimmer.client.ApiIgnore
import org.babyfish.jimmer.client.meta.Doc
import org.babyfish.jimmer.dto.compiler.AbstractProp
import org.babyfish.jimmer.dto.compiler.Anno
import org.babyfish.jimmer.dto.compiler.Anno.AnnoValue
import org.babyfish.jimmer.dto.compiler.Anno.ArrayValue
import org.babyfish.jimmer.dto.compiler.Anno.EnumValue
import org.babyfish.jimmer.dto.compiler.Anno.LiteralValue
import org.babyfish.jimmer.dto.compiler.Anno.TypeRefValue
import org.babyfish.jimmer.dto.compiler.Anno.Value
import org.babyfish.jimmer.dto.compiler.Constants
import org.babyfish.jimmer.dto.compiler.DtoModifier
import org.babyfish.jimmer.dto.compiler.DtoProp
import org.babyfish.jimmer.dto.compiler.DtoType
import org.babyfish.jimmer.dto.compiler.LikeOption
import org.babyfish.jimmer.dto.compiler.PropConfig.PathNode
import org.babyfish.jimmer.dto.compiler.PropConfig.Predicate
import org.babyfish.jimmer.dto.compiler.PropConfig.Predicate.And
import org.babyfish.jimmer.dto.compiler.PropConfig.Predicate.Cmp
import org.babyfish.jimmer.dto.compiler.PropConfig.Predicate.Nullity
import org.babyfish.jimmer.dto.compiler.PropConfig.Predicate.Or
import org.babyfish.jimmer.dto.compiler.TypeRef
import org.babyfish.jimmer.dto.compiler.UserProp
import org.babyfish.jimmer.impl.util.StringUtil
import org.babyfish.jimmer.impl.util.StringUtil.SnakeCase
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.annotation
import org.babyfish.jimmer.ksp.client.DocMetadata
import org.babyfish.jimmer.ksp.fullName
import org.babyfish.jimmer.ksp.get
import org.babyfish.jimmer.ksp.immutable.generator.BIG_DECIMAL_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.BIG_INTEGER_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.CLASS_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.DESCRIPTION_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.DTO_METADATA_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.DTO_PROP_ACCESSOR
import org.babyfish.jimmer.ksp.immutable.generator.EMBEDDED_DTO_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.FIXED_INPUT_FIELD_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.HIBERNATE_VALIDATOR_ENHANCED_BEAN
import org.babyfish.jimmer.ksp.immutable.generator.INPUT_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.JSON_CREATOR_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.JSON_DESERIALIZE_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.JSON_IGNORE_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.JSON_PROPERTY_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.JSON_SERIALIZE_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.JVM_STATIC_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.K_SPECIFICATION_ARGS_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.K_SPECIFICATION_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.NEW_FETCHER_FUN_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.PREDICATE_APPLIER
import org.babyfish.jimmer.ksp.immutable.generator.REFERENCE_FETCH_TYPE_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.VIEW_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType
import org.babyfish.jimmer.ksp.util.ConverterMetadata
import org.babyfish.jimmer.ksp.util.GenericParser
import org.babyfish.jimmer.ksp.util.fastResolve
import org.babyfish.jimmer.ksp.util.generatedAnnotation
import org.babyfish.jimmer.ksp.util.toPoetTarget
import java.io.OutputStreamWriter
import java.util.*
import kotlin.math.min

class DtoGenerator private constructor(
    private val ctx: Context,
    private val docMetadata: DocMetadata,
    private val mutable: Boolean,
    val dtoType: DtoType<ImmutableType, ImmutableProp>,
    private val codeGenerator: CodeGenerator?,
    private val parent: DtoGenerator?,
    private val innerClassName: String?,
) {
    private val root: DtoGenerator = parent?.root ?: this

    private val document: Document = Document()

    private val useSiteTargetMap = mutableMapOf<String, Set<AnnotationUseSiteTarget>>()

    private val interfacePropNames = abstractPropNames(ctx, dtoType)

    init {
        if ((codeGenerator === null) == (parent === null)) {
            throw IllegalArgumentException("The nullity values of `codeGenerator` and `parent` cannot be same")
        }
        if ((parent === null) != (innerClassName === null)) {
            throw IllegalArgumentException("The nullity values of `parent` and `innerClassName` must be same")
        }
    }

    private var _typeBuilder: TypeSpec.Builder? = null

    constructor(
        ctx: Context,
        docMetadata: DocMetadata,
        mutable: Boolean,
        dtoType: DtoType<ImmutableType, ImmutableProp>,
        codeGenerator: CodeGenerator?,
    ) : this(ctx, docMetadata, mutable, dtoType, codeGenerator, null, null)

    val typeBuilder: TypeSpec.Builder
        get() = _typeBuilder ?: error("Type builder is not ready")

    fun getDtoClassName(nestedSimpleName: String? = null): ClassName {
        if (innerClassName !== null) {
            val list: MutableList<String> = ArrayList()
            collectNames(list)
            return ClassName(
                root.dtoType.packageName,
                list[0],
                *list.subList(1, list.size).let {
                    if (nestedSimpleName == null) {
                        it
                    } else {
                        it.toMutableList() + nestedSimpleName
                    }
                }.toTypedArray()
            )
        }
        if (nestedSimpleName == null) {
            return ClassName(
                root.dtoType.packageName,
                dtoType.name!!
            )
        }
        return ClassName(
            root.dtoType.packageName,
            dtoType.name!!,
            nestedSimpleName
        )
    }

    fun generate(allFiles: List<KSFile>) {
        if (codeGenerator != null) {
            codeGenerator.createNewFile(
                Dependencies(false, *allFiles.toTypedArray()),
                root.dtoType.packageName,
                dtoType.name!!
            ).use {
                val fileSpec = FileSpec
                    .builder(
                        root.dtoType.packageName,
                        dtoType.name!!
                    ).apply {
                        indent("    ")
                        addImports()
                        val builder = TypeSpec
                            .classBuilder(dtoType.name!!)
                            .addModifiers(KModifier.OPEN)
                        if (parent == null) {
                            builder.addAnnotation(generatedAnnotation(dtoType.dtoFile, mutable))
                        }
                        builder.addTypeAnnotations()
                        _typeBuilder = builder
                        try {
                            addDoc()
                            addMembers()
                            addType(builder.build())
                            addExtensions()
                        } finally {
                            _typeBuilder = null
                        }
                    }.build()
                val writer = OutputStreamWriter(it, Charsets.UTF_8)
                fileSpec.writeTo(writer)
                writer.flush()
            }
        } else if (innerClassName !== null && parent !== null) {
            val builder = TypeSpec
                .classBuilder(innerClassName)
                .addModifiers(KModifier.OPEN)
                .addAnnotation(generatedAnnotation())
            builder.addTypeAnnotations()
            _typeBuilder = builder
            try {
                addDoc()
                addMembers()
                parent.typeBuilder.addType(builder.build())
            } finally {
                _typeBuilder = null
            }
        }
    }

    private fun FileSpec.Builder.addImports() {
        val packages = sortedSetOf<String>().also {
            collectImports(dtoType, it)
        }
        for (pkg in packages) {
            addImport(pkg, "by")
        }
    }

    private fun collectImports(
        dtoType: DtoType<ImmutableType, ImmutableProp>,
        packages: SortedSet<String>,
    ) {
        packages += dtoType.baseType.className.packageName
        for (prop in dtoType.dtoProps) {
            val targetType = prop.targetType
            if (targetType !== null && (!prop.isRecursive || targetType.isFocusedRecursion)) {
                collectImports(targetType, packages)
            } else {
                prop.baseProp.targetType?.className?.packageName?.let {
                    packages += it
                }
            }
        }
    }

    private fun TypeSpec.Builder.addTypeAnnotations() {
        for (anno in dtoType.baseType.classDeclaration.annotations) {
            if (isCopyableAnnotation(anno, dtoType.annotations)) {
                addAnnotation(anno.toAnnotationSpec())
            }
        }
        for (anno in dtoType.annotations) {
            if (anno.qualifiedName != KOTLIN_DTO_TYPE_NAME) {
                addAnnotation(annotationOf(anno))
            }
        }
    }

    private fun addDoc() {
        (document.value ?: baseDocString)?.let {
            typeBuilder.addAnnotation(
                AnnotationSpec
                    .builder(DESCRIPTION_CLASS_NAME)
                    .addMember("value = %S", it)
                    .build()
            )
        }
    }

    private fun addMembers() {
        if (isSerializerRequired) {
            typeBuilder.addAnnotation(
                AnnotationSpec
                    .builder(JSON_SERIALIZE_CLASS_NAME)
                    .addMember("using = %T::class", getDtoClassName("Serializer"))
                    .build()
            )
        }
        if (isBuilderRequired) {
            typeBuilder.addAnnotation(
                AnnotationSpec
                    .builder(JSON_DESERIALIZE_CLASS_NAME)
                    .addMember("builder = %T::class", getDtoClassName("Builder"))
                    .build()
            )
        }
        val isSpecification = dtoType.modifiers.contains(DtoModifier.SPECIFICATION)
        if (isImpl && dtoType.baseType.isEntity) {
            typeBuilder.addSuperinterface(
                when {
                    isSpecification ->
                        K_SPECIFICATION_CLASS_NAME

                    dtoType.modifiers.contains(DtoModifier.INPUT) ->
                        INPUT_CLASS_NAME

                    else ->
                        VIEW_CLASS_NAME
                }.parameterizedBy(
                    dtoType.baseType.className
                )
            )
        }
        if (isImpl && dtoType.baseType.isEmbeddable) {
            typeBuilder.addSuperinterface(
                EMBEDDED_DTO_CLASS_NAME.parameterizedBy(
                    dtoType.baseType.className
                )
            )
        }
        for (typeRef in dtoType.superInterfaces) {
            typeBuilder.addSuperinterface(typeName(typeRef))
        }
        if (isHibernateValidatorEnhancementRequired) {
            typeBuilder.addSuperinterface(HIBERNATE_VALIDATOR_ENHANCED_BEAN)
        }

        addPrimaryConstructor()
        if (!isSpecification) {
            addConverterConstructor()
        }

        for (prop in dtoType.dtoProps) {
            addProp(prop)
            addStateProp(prop)
        }
        for (prop in dtoType.userProps) {
            addProp(prop)
        }

        if (isSpecification) {
            addEntityType()
            addApplyTo()
        } else {
            addToEntity()
            addToEntityEx()
            addToEntityImpl()
        }

        for (prop in dtoType.dtoProps) {
            typeBuilder.addSpecificationConverter(prop)
        }

        typeBuilder.addCopy()
        typeBuilder.addHashCode()
        typeBuilder.addEquals()
        typeBuilder.addToString()

        if (!isSpecification) {
            typeBuilder.addType(
                TypeSpec
                    .companionObjectBuilder()
                    .addAnnotation(generatedAnnotation())
                    .apply {
                        addMetadata()
                        for (prop in dtoType.dtoProps) {
                            addAccessorField(prop)
                        }
                    }
                    .build()
            )
        }

        for (prop in dtoType.dtoProps) {
            val targetType = prop.targetType ?: continue
            if (!prop.isRecursive || targetType.isFocusedRecursion) {
                DtoGenerator(
                    ctx,
                    docMetadata,
                    mutable,
                    targetType,
                    null,
                    this,
                    targetSimpleName(prop)
                ).generate(emptyList())
            }
        }

        if (isHibernateValidatorEnhancementRequired) {
            typeBuilder.addHibernateValidatorEnhancement(false)
            typeBuilder.addHibernateValidatorEnhancement(true)
        }
        if (isSerializerRequired) {
            SerializerGenerator(this).generate()
        }
        if (isBuilderRequired) {
            InputBuilderGenerator(this).generate()
        }
    }

    private fun FileSpec.Builder.addExtensions() {
        if (!dtoType.modifiers.contains(DtoModifier.SPECIFICATION)) {
            addToEntities()
            addToEntitiesEx()
        }
    }

    private fun TypeSpec.Builder.addMetadata() {
        addProperty(
            PropertySpec
                .builder(
                    "METADATA",
                    DTO_METADATA_CLASS_NAME.parameterizedBy(
                        dtoType.baseType.className,
                        getDtoClassName()
                    )
                )
                .addAnnotation(JVM_STATIC_CLASS_NAME)
                .initializer(
                    CodeBlock
                        .builder()
                        .apply {
                            add("\n")
                            indent()
                            add(
                                "%T<%T, %T>(\n",
                                DTO_METADATA_CLASS_NAME,
                                dtoType.baseType.className, getDtoClassName()
                            )
                            indent()
                            metadataFetcherExpr()
                            add(",\n::%T\n", getDtoClassName())
                            unindent()
                            add(")")
                            unindent()
                        }
                        .build()
                )
                .build()
        )
    }

    private fun CodeBlock.Builder.metadataFetcherExpr() {
        add(
            "%T(%T::class).by {\n",
            NEW_FETCHER_FUN_CLASS_NAME,
            dtoType.baseType.className
        )
        indent()
        for (prop in dtoType.dtoProps) {
            if (prop.nextProp === null) {
                addFetcherField(prop)
            }
        }
        for (hiddenFlatProp in dtoType.hiddenFlatProps) {
            if (!hiddenFlatProp.baseProp.isId) {
                addHiddenFetcherField(hiddenFlatProp)
            }
        }
        unindent()
        add("}")
    }

    private fun CodeBlock.Builder.addFetcherField(prop: DtoProp<ImmutableType, ImmutableProp>) {
        if (!prop.baseProp.isId) {
            if (prop.targetType !== null) {
                if (prop.isRecursive) {
                    add("`%L*`", prop.baseProp.name)
                    if (prop.config == null) {
                        add("()")
                    }
                } else {
                    add(
                        "%L(%T.METADATA.fetcher)",
                        prop.baseProp.name,
                        propElementName(prop)
                    )
                }
            } else {
                add("%L", prop.baseProp.name)
                if (prop.config == null) {
                    add("()")
                }
            }
            addConfigLambda(prop)
            add("\n")
        }
    }

    private fun CodeBlock.Builder.addConfigLambda(
        prop: DtoProp<ImmutableType, ImmutableProp>,
    ) {
        val cfg = prop.getConfig() ?: return
        add(" {")
        indent()
        when {
            cfg.predicate != null || cfg.orderItems.isNotEmpty() -> {
                add("\nfilter {")
                indent()
                cfg.predicate?.let {
                    val realPredicates = if (it is And) {
                        it.predicates
                    } else {
                        listOf(it)
                    }
                    for (realPredicate in realPredicates) {
                        add("\nwhere(\n")
                        indent()
                        addPredicate(realPredicate)
                        unindent()
                        add("\n)")
                    }
                }
                cfg.orderItems.takeIf { it.isNotEmpty() }?.let {
                    add("\norderBy(")
                    indent()
                    for (i in it.indices) {
                        if (i != 0) {
                            add(", ")
                        }
                        add("\n")
                        addPropPath(it[i].path)
                        if (it[i].isDesc) {
                            add(".%M()", MemberName(EXPRESSION_PACKAGE, "desc"))
                        } else {
                            add(".%M()", MemberName(EXPRESSION_PACKAGE, "asc"))
                        }
                    }
                    unindent()
                    add("\n)")
                }
                unindent()
                add("\n}")
            }

            cfg.filterClassName != null -> {
                val fetcherDeclaration = ctx.resolver.getClassDeclarationByName(cfg.filterClassName!!)
                    ?: throw DtoException(
                        "There is no filter class: ${cfg.filterClassName}"
                    )
                val entityTypeName = GenericParser(
                    "filter",
                    fetcherDeclaration,
                    "org.babyfish.jimmer.sql.kt.fetcher.KFieldFilter"
                ).parse().argumentTypeNames[0]
                val targetTypeName = prop.toTailProp().baseProp.targetTypeName(overrideNullable = false)
                if (entityTypeName != targetTypeName) {
                    throw DtoException(
                        "The filter class \"" +
                                cfg.filterClassName +
                                "\" is illegal, it specify the generic type argument of \"" +
                                "org.babyfish.jimmer.sql.kt.fetcher.KFieldFilter" +
                                "\" as \"" +
                                entityTypeName +
                                "\", which is not associated entity type \"" +
                                targetTypeName +
                                "\""
                    )
                }
                add("\nfilter(%L())", cfg.filterClassName)
            }
        }
        if (cfg.recursionClassName !== null) {
            val recursionDeclaration = ctx.resolver.getClassDeclarationByName(cfg.recursionClassName!!)
                ?: throw DtoException(
                    "There is no recursion class: ${cfg.recursionClassName}"
                )
            val entityTypeName = GenericParser(
                "recursion",
                recursionDeclaration,
                "org.babyfish.jimmer.sql.fetcher.RecursionStrategy"
            ).parse().argumentTypeNames[0]
            val targetTypeName = prop.toTailProp().baseProp.targetTypeName(overrideNullable = false)
            if (entityTypeName != targetTypeName) {
                throw DtoException(
                    "The recursion class \"" +
                            cfg.recursionClassName +
                            "\" is illegal, it specify the generic type argument of \"" +
                            "org.babyfish.jimmer.sql.fetcher.RecursionStrategy" +
                            "\" as \"" +
                            entityTypeName +
                            "\", which is not associated entity type \"" +
                            targetTypeName +
                            "\""
                )
            }
            add("\nrecursive(%L())", cfg.recursionClassName)
        }
        if (cfg.fetchType !== "AUTO") {
            add("\nfetchType(%T.%L)", REFERENCE_FETCH_TYPE_CLASS_NAME, cfg.fetchType)
        }
        if (cfg.limit != Int.MAX_VALUE) {
            if (cfg.offset != 0) {
                add("\nlimit(%L, %L)", cfg.limit, cfg.offset)
            } else {
                add("\nlimit(%L)", cfg.limit)
            }
        }
        if (cfg.batch != 0) {
            add("\nbatch(%L)", cfg.batch)
        }
        if (cfg.depth != Int.MAX_VALUE) {
            add("\ndepth(%L)", cfg.depth)
        }
        unindent()
        add("\n}")
    }

    @Suppress("UNCHECKED_CAST")
    private fun CodeBlock.Builder.addPredicate(predicate: Predicate) {
        when (predicate) {
            is And -> {
                add("%M(\n", MemberName(EXPRESSION_PACKAGE, "and"))
                indent()
                for (i in predicate.predicates.indices) {
                    if (i != 0) {
                        add(",\n")
                    }
                    addPredicate(predicate.predicates[i])
                }
                unindent()
                add("\n)")
            }

            is Or -> {
                add("%M(\n", MemberName(EXPRESSION_PACKAGE, "or"))
                indent()
                for (i in predicate.predicates.indices) {
                    if (i != 0) {
                        add(",\n")
                    }
                    addPredicate(predicate.predicates[i])
                }
                unindent()
                add("\n)")
            }

            is Cmp<*> -> {
                addPropPath(predicate.path as List<PathNode<ImmutableProp>>)
                val ktOp = MemberName(
                    EXPRESSION_PACKAGE,
                    when (predicate.operator) {
                        "=" -> "eq"
                        "<>" -> "ne"
                        "<" -> "lt"
                        "<=" -> "le"
                        ">" -> "gt"
                        ">=" -> "ge"
                        else -> predicate.operator
                    }
                )
                if (predicate.value is String) {
                    add(" %M %S", ktOp, predicate.value)
                } else {
                    val prop = predicate.path[predicate.path.size - 1].prop as ImmutableProp
                    when (prop.typeName(overrideNullable = false)) {
                        LONG -> add(" %M %LL", ktOp, predicate.value)
                        FLOAT -> add(" %M %LF", ktOp, predicate.value)
                        DOUBLE -> add(" %M %LD", ktOp, predicate.value)
                        BIG_INTEGER_CLASS_NAME -> add(
                            " %M %T(%S)",
                            ktOp,
                            BIG_INTEGER_CLASS_NAME,
                            predicate.value
                        )

                        BIG_DECIMAL_CLASS_NAME -> add(
                            " %M %T(%S)",
                            ktOp,
                            BIG_DECIMAL_CLASS_NAME,
                            predicate.value
                        )

                        else -> add(" %M %L", ktOp, predicate.value)
                    }
                }
            }

            is Nullity<*> -> {
                addPropPath(predicate.path as List<PathNode<ImmutableProp>>)
                if (predicate.isNegative) {
                    add(".%M()", MemberName(EXPRESSION_PACKAGE, "isNotNull"))
                } else {
                    add(".%M()", MemberName(EXPRESSION_PACKAGE, "isNull"))
                }
            }

            else -> throw DtoException("Illegal predicate type: ${predicate::class.qualifiedName}")
        }
    }

    private fun CodeBlock.Builder.addPropPath(pathNodes: List<PathNode<ImmutableProp>>) {
        add("table")
        for (pathNode in pathNodes) {
            val prop = pathNode.prop
            val packageName = prop.declaringType.packageName
            val name = if (pathNode.isAssociatedId) {
                "${prop.name}Id"
            } else {
                prop.name
            }
            add(".%M", MemberName(packageName, name))
        }
    }

    private fun CodeBlock.Builder.addHiddenFetcherField(prop: DtoProp<ImmutableType, ImmutableProp>) {
        if ("flat" != prop.getFuncName()) {
            addFetcherField(prop)
            return
        }
        val targetDtoType = prop.getTargetType()!!
        add("%L {\n", prop.baseProp.name)
        indent()
        for (childProp in targetDtoType.dtoProps) {
            addHiddenFetcherField(childProp)
        }
        unindent()
        add("\n}\n")
    }

    private fun addStateProp(prop: DtoProp<ImmutableType, ImmutableProp>) {
        statePropName(prop, false)?.let {
            typeBuilder.addProperty(
                PropertySpec
                    .builder(it, BOOLEAN)
                    .addAnnotation(ApiIgnore::class)
                    .addAnnotation(
                        AnnotationSpec
                            .builder(JSON_IGNORE_CLASS_NAME)
                            .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                            .build()
                    )
                    .mutable(mutable)
                    .initializer(it)
                    .build()
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun addProp(prop: AbstractProp) {
        val typeName = propTypeName(prop)
        typeBuilder.addProperty(
            PropertySpec
                .builder(prop.name, typeName)
                .mutable(mutable)
                .apply {
                    if (interfacePropNames.contains(prop.name)) {
                        addModifiers(KModifier.OVERRIDE)
                    }
                    val doc = document[prop]
                        ?: prop.takeIf { it !is DtoProp<*, *> || it.nextProp === null }
                            ?.doc
                    doc?.let {
                        addAnnotation(
                            AnnotationSpec
                                .builder(DESCRIPTION_CLASS_NAME)
                                .addMember("value = %S", it)
                                .build()
                        )
                    }
                    if (!isBuilderRequired && prop.annotations.none { it.qualifiedName == JSON_PROPERTY_TYPE_NAME }) {
                        addAnnotation(
                            AnnotationSpec
                                .builder(JSON_PROPERTY_CLASS_NAME)
                                .apply {
                                    addMember("%S", prop.name)
                                    if (!prop.isNullable) {
                                        addMember("required = true")
                                    }
                                }
                                .build()
                        )
                    }
                    if (prop is DtoProp<*, *>) {
                        val dtoProp = prop as DtoProp<ImmutableType, ImmutableProp>
                        if (dtoType.modifiers.contains(DtoModifier.INPUT) && dtoProp.inputModifier == DtoModifier.FIXED) {
                            addAnnotation(FIXED_INPUT_FIELD_CLASS_NAME)
                        }
                        for (anno in dtoProp.toTailProp().baseProp.annotations {
                            isCopyableAnnotation(it, dtoProp.annotations)
                        }) {
                            if (isBuilderRequired && anno.fullName == JSON_DESERIALIZE_TYPE_NAME) {
                                continue
                            }
                            allowedTargets(anno.fullName).firstOrNull()?.let {
                                addAnnotation(
                                    standardSpec(
                                        object : KSAnnotation by anno {
                                            override val useSiteTarget: AnnotationUseSiteTarget
                                                get() = it
                                        }.toAnnotationSpec()
                                    )
                                )
                            }
                        }
                    }
                    for (anno in prop.annotations) {
                        if (isBuilderRequired && anno.qualifiedName == JSON_DESERIALIZE_TYPE_NAME) {
                            continue
                        }
                        val target = if (anno.qualifiedName.startsWith("com.fasterxml.jackson.")) {
                            AnnotationUseSiteTarget.GET
                        } else {
                            allowedTargets(anno.qualifiedName).firstOrNull() ?: continue
                        }
                        addAnnotation(
                            standardSpec(
                                annotationOf(anno, target.toPoetTarget())
                            )
                        )
                    }
                    initializer(prop.name)
                    if (mutable) {
                        statePropName(prop, false)?.let { stateProp ->
                            val name = prop.name.takeIf { it != "field" } ?: "value"
                            setter(
                                FunSpec
                                    .setterBuilder()
                                    .addParameter(name, typeName)
                                    .addStatement("field = %L", name)
                                    .addStatement("%L = true", stateProp)
                                    .build()
                            )
                        }
                    }
                }
                .build()
        )
    }

    private fun addPrimaryConstructor() {
        typeBuilder.primaryConstructor(
            FunSpec
                .constructorBuilder()
                .apply {
                    if (!isBuilderRequired) {
                        addAnnotation(JSON_CREATOR_CLASS_NAME)
                    }
                    for (prop in dtoType.dtoProps) {
                        addParameter(
                            ParameterSpec.builder(prop.name, propTypeName(prop))
                                .apply {
                                    if (prop.isNullable) {
                                        defaultValue("null")
                                    } else if (propTypeName(prop) == BOOLEAN) {
                                        defaultValue("false")
                                    }
                                }
                                .build()
                        )
                        statePropName(prop, false)?.let {
                            addParameter(
                                ParameterSpec
                                    .builder(
                                        StringUtil.identifier("is", prop.name, "Loaded"),
                                        BOOLEAN
                                    )
                                    .apply {
                                        if (prop.isNullable) {
                                            defaultValue("%L !== null", prop.name)
                                        } else {
                                            defaultValue("true")
                                        }
                                    }
                                    .build()
                            )
                        }
                    }
                    for (prop in dtoType.userProps) {
                        addParameter(
                            ParameterSpec.builder(prop.name, typeName(prop.typeRef))
                                .apply {
                                    if (prop.defaultValueText !== null) {
                                        defaultValue(prop.defaultValueText!!)
                                    } else if (prop.isNullable) {
                                        defaultValue("null")
                                    }
                                }
                                .build()
                        )
                    }
                }
                .build()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun addConverterConstructor() {
        typeBuilder.addFunction(
            FunSpec
                .constructorBuilder()
                .addParameter("base", dtoType.baseType.className)
                .apply {
                    for (userProp in dtoType.userProps) {
                        addParameter(
                            ParameterSpec
                                .builder(userProp.alias, typeName(userProp.typeRef))
                                .apply {
                                    defaultValue(userProp)?.let {
                                        defaultValue(it)
                                    }
                                }
                                .build()
                        )
                    }
                }
                .callThisConstructor(dtoType.props.map { prop ->
                    CodeBlock
                        .builder()
                        .indent()
                        .add("\n")
                        .apply {
                            if (prop is DtoProp<*, *>) {
                                if (isSimpleProp(prop as DtoProp<ImmutableType, ImmutableProp>)) {
                                    add("base.%L", prop.baseProp.name)
                                } else if (!prop.isNullable && prop.isBaseNullable) {
                                    add(
                                        "%L.get<%T>(\n",
                                        StringUtil.snake("${prop.name}Accessor", SnakeCase.UPPER),
                                        propTypeName(prop)
                                    )
                                    indent()
                                    add("base,\n")
                                    add(
                                        "%S\n",
                                        "Cannot convert \"${dtoType.baseType.className}\" to " +
                                                "\"${getDtoClassName()}\" because the cannot get non-null " +
                                                "value for \"${prop.name}\""
                                    )
                                    unindent()
                                    add(")")
                                } else {
                                    add(
                                        "%L.get<%T>(base)",
                                        StringUtil.snake("${prop.name}Accessor", SnakeCase.UPPER),
                                        propTypeName(prop)
                                    )
                                }
                                statePropName(prop, false)?.let {
                                    if (isSimpleProp(prop as DtoProp<ImmutableType, ImmutableProp>)) {
                                        add(
                                            ",\n%T.%L.isLoaded(base)",
                                            dtoType.baseType.propsClassName,
                                            StringUtil.snake(prop.baseProp.name, SnakeCase.UPPER)
                                        )
                                    } else {
                                        add(
                                            ",\n%L.isLoaded(base)\n",
                                            StringUtil.snake("${prop.name}Accessor", SnakeCase.UPPER)
                                        )
                                    }
                                }
                            } else {
                                add("%N", prop.alias)
                            }
                        }
                        .unindent()
                        .build()
                })
                .build()
        )
    }

    private fun addToEntity() {
        typeBuilder.addFunction(
            FunSpec
                .builder(if (dtoType.baseType.isEntity) "toEntity" else "toImmutable")
                .addModifiers(KModifier.OVERRIDE)
                .returns(dtoType.baseType.className)
                .apply {
                    addStatement(
                        "return %M(%T::class).by(null, false, this@%L::%L)",
                        NEW,
                        dtoType.baseType.className,
                        innerClassName ?: dtoType.name!!,
                        if (dtoType.baseType.isEntity) "toEntityImpl" else "toImmutableImpl"
                    )
                }
                .build()
        )
    }

    private fun FileSpec.Builder.addToEntities() {
        val dtoClassName = getDtoClassName()
        addFunction(
            FunSpec
                .builder(if (dtoType.baseType.isEntity) "toEntities" else "toImmutables")
                .addAnnotation(generatedAnnotation(dtoType.baseType.className))
                .receiver(ITERABLE.parameterizedBy(dtoClassName))
                .returns(LIST.parameterizedBy(dtoType.baseType.className))
                .addStatement(
                    "return map(%T::%L)",
                    dtoClassName,
                    if (dtoType.baseType.isEntity) "toEntity" else "toImmutable"
                )
                .build()
        )
    }

    private fun FileSpec.Builder.addToEntitiesEx() {
        addFunction(
            FunSpec
                .builder(if (dtoType.baseType.isEntity) "toEntities" else "toImmutables")
                .addAnnotation(generatedAnnotation(dtoType.baseType.className))
                .receiver(ITERABLE.parameterizedBy(getDtoClassName()))
                .returns(LIST.parameterizedBy(dtoType.baseType.className))
                .addParameter(
                    "block",
                    LambdaTypeName.get(
                        dtoType.baseType.draftClassName,
                        emptyList(),
                        UNIT
                    ),
                )
                .apply {
                    beginControlFlow("return map")
                    addStatement(
                        "it.%L(block)",
                        if (dtoType.baseType.isEntity) "toEntity" else "toImmutable"
                    )
                    endControlFlow()
                }
                .build()
        )
    }

    private fun addToEntityEx() {
        typeBuilder.addFunction(
            FunSpec
                .builder(if (dtoType.baseType.isEntity) "toEntity" else "toImmutable")
                .addParameter(
                    "block",
                    LambdaTypeName.get(
                        dtoType.baseType.draftClassName,
                        emptyList(),
                        UNIT
                    ),
                )
                .returns(dtoType.baseType.className)
                .apply {
                    beginControlFlow(
                        "return %M(%T::class).by",
                        NEW,
                        dtoType.baseType.className
                    )
                    addStatement(
                        "%L(this)",
                        if (dtoType.baseType.isEntity) "toEntityImpl" else "toImmutableImpl"
                    )
                    addStatement("block(this)")
                    endControlFlow()
                }
                .build()
        )
    }

    private fun addToEntityImpl() {
        typeBuilder.addFunction(
            FunSpec
                .builder(if (dtoType.baseType.isEntity) "toEntityImpl" else "toImmutableImpl")
                .addKdoc(DOC_EXPLICIT_FUN)
                .addModifiers(KModifier.PRIVATE)
                .addParameter("_draft", dtoType.baseType.draftClassName)
                .apply {
                    for (prop in dtoType.dtoProps) {
                        val baseProp = prop.toTailProp().baseProp
                        if (baseProp.isKotlinFormula) {
                            continue
                        }
                        val statePropName = statePropName(prop, false)
                        if (statePropName !== null) {
                            beginControlFlow("if (%L)", statePropName)
                            addDraftAssignment(prop, prop.name)
                            endControlFlow()
                        } else {
                            addDraftAssignment(prop, prop.name)
                        }
                    }
                }
                .build()
        )
    }

    private fun FunSpec.Builder.addDraftAssignment(prop: DtoProp<ImmutableType, ImmutableProp>, valueExpr: String) {
        val baseProp = prop.toTailProp().baseProp
        if (isSimpleProp(prop)) {
            addStatement("_draft.%L = %L", baseProp.name, valueExpr)
        } else {
            if (prop.isNullable && baseProp.let { it.isList && it.isAssociation(true) }) {
                addStatement(
                    "%L.set(_draft, %L)",
                    StringUtil.snake("${prop.name}Accessor", SnakeCase.UPPER),
                    valueExpr
                )
            } else {
                addStatement(
                    "%L.set(_draft, %L)",
                    StringUtil.snake("${prop.name}Accessor", SnakeCase.UPPER),
                    valueExpr
                )
            }
        }
    }

    private fun addEntityType() {
        typeBuilder.addFunction(
            FunSpec
                .builder("entityType")
                .apply {
                    if (isImpl) {
                        addModifiers(KModifier.OVERRIDE)
                    }
                }
                .returns(
                    CLASS_CLASS_NAME.parameterizedBy(
                        dtoType.baseType.className
                    )
                )
                .addStatement("return %T::class.java", dtoType.baseType.className)
                .build()
        )
    }

    private fun addApplyTo() {
        typeBuilder.addFunction(
            FunSpec
                .builder("applyTo")
                .apply {
                    if (isImpl) {
                        addParameter(
                            "args",
                            K_SPECIFICATION_ARGS_CLASS_NAME.parameterizedBy(dtoType.baseType.className)
                        )
                        addModifiers(KModifier.OVERRIDE)
                        addStatement("val _applier = args.applier")
                    } else {
                        addParameter(
                            "_applier",
                            PREDICATE_APPLIER
                        )
                    }
                    var stack = emptyList<ImmutableProp>()
                    for (prop in dtoType.dtoProps) {
                        val newStack = mutableListOf<ImmutableProp>()
                        val tailProp = prop.toTailProp()
                        var p: DtoProp<ImmutableType, ImmutableProp>? = prop
                        while (p != null) {
                            if (p !== tailProp || p.getTargetType() != null) {
                                newStack.add(p.getBaseProp())
                            }
                            p = p.getNextProp()
                        }
                        stack = addStackOperations(stack, newStack)
                        addPredicateOperation(prop)
                    }
                    addStackOperations(stack, emptyList())
                }
                .build()
        )
    }

    private fun FunSpec.Builder.addStackOperations(
        stack: List<ImmutableProp>,
        newStack: List<ImmutableProp>,
    ): List<ImmutableProp> {
        val size = min(stack.size, newStack.size)
        var sameCount = size
        for (i in 0 until size) {
            if (stack[i] !== newStack[i]) {
                sameCount = i
                break
            }
        }
        for (i in stack.size - sameCount downTo 1) {
            addStatement("_applier.pop()")
        }
        for (prop in newStack.subList(sameCount, newStack.size)) {
            addStatement(
                "_applier.push(%T.%L.unwrap())",
                prop.declaringType.propsClassName,
                StringUtil.snake(prop.name, SnakeCase.UPPER)
            )
        }
        return newStack
    }

    private fun FunSpec.Builder.addPredicateOperation(prop: DtoProp<ImmutableType, ImmutableProp>) {
        val propName = prop.name
        val tailProp = prop.toTailProp()
        val targetType = tailProp.targetType
        if (targetType !== null) {
            if (targetType.baseType.isEntity) {
                addStatement("this.%L?.let { it.applyTo(args.child()) }", propName)
            } else {
                addStatement("this.%L?.let { it.applyTo(args.applier) }", propName)
            }
            return
        }

        val funcName = when (tailProp.funcName) {
            null -> "eq"
            "id" -> "associatedIdEq"
            else -> tailProp.funcName
        }
        val ktFunName = when (funcName) {
            "null" -> "isNull"
            "notNull" -> "isNotNull"
            else -> funcName
        }

        addCode(
            CodeBlock.builder()
                .apply {
                    add("_applier.%L(", ktFunName)
                    if (Constants.MULTI_ARGS_FUNC_NAMES.contains(funcName)) {
                        add("arrayOf(")
                        tailProp.basePropMap.values.forEachIndexed { index, baseProp ->
                            if (index != 0) {
                                add(", ")
                            }
                            add(
                                "%T.%L.unwrap()",
                                baseProp.declaringType.propsClassName,
                                StringUtil.snake(baseProp.name, SnakeCase.UPPER)
                            )
                        }
                        add(")")
                    } else {
                        add(
                            "%T.%L.unwrap()",
                            tailProp.baseProp.declaringType.propsClassName,
                            StringUtil.snake(tailProp.baseProp.name, SnakeCase.UPPER)
                        )
                    }
                    if (isSpecificationConverterRequired(tailProp)) {
                        add(
                            ", %L(this.%L)",
                            StringUtil.identifier("_convert", propName),
                            propName
                        )
                    } else {
                        add(", this.%L", propName)
                    }
                    if (funcName == "like") {
                        add(", ")
                        add(if (tailProp.likeOptions.contains(LikeOption.INSENSITIVE)) "true" else "false")
                        add(", ")
                        add(if (tailProp.likeOptions.contains(LikeOption.MATCH_START)) "true" else "false")
                        add(", ")
                        add(if (tailProp.likeOptions.contains(LikeOption.MATCH_END)) "true" else "false")
                    }
                    add(")\n")
                }
                .build()
        )
    }

    private fun isSimpleProp(prop: DtoProp<ImmutableType, ImmutableProp>): Boolean {
        if (prop.getNextProp() != null) {
            return false
        }
        return if ((prop.isNullable() && (!prop.getBaseProp().isNullable || dtoType.modifiers.contains(DtoModifier.SPECIFICATION))) ||
            (prop.baseProp.converterMetadata !== null &&
                    !dtoType.modifiers.contains(DtoModifier.INPUT) &&
                    !dtoType.modifiers.contains(DtoModifier.SPECIFICATION))
        ) {
            false
        } else {
            propTypeName(prop) == prop.getBaseProp().typeName()
        }
    }

    private fun TypeSpec.Builder.addAccessorField(prop: DtoProp<ImmutableType, ImmutableProp>) {
        if (isSimpleProp(prop)) {
            return
        }

        val builder = PropertySpec.builder(
            StringUtil.snake("${prop.name}Accessor", SnakeCase.UPPER),
            DTO_PROP_ACCESSOR,
            KModifier.PRIVATE
        ).initializer(
            CodeBlock
                .builder()
                .apply {
                    add("%T(", DTO_PROP_ACCESSOR)
                    indent()

                    if (prop.isNullable() && (!prop.toTailProp().getBaseProp().isNullable ||
                                dtoType.modifiers.contains(DtoModifier.SPECIFICATION) ||
                                dtoType.modifiers.contains(DtoModifier.FUZZY) ||
                                prop.inputModifier == DtoModifier.FUZZY
                                )
                    ) {
                        add("\nfalse")
                    } else {
                        add("\ntrue")
                    }

                    if (prop.nextProp === null) {
                        add(
                            ",\nintArrayOf(%T.%L)",
                            dtoType.baseType.draftClassName("$"),
                            prop.baseProp.slotName
                        )
                    } else {
                        add(",\nintArrayOf(")
                        indent()
                        var p: DtoProp<ImmutableType, ImmutableProp>? = prop
                        while (p !== null) {
                            if (p !== prop) {
                                add(",")
                            }
                            add(
                                "\n%T.%L",
                                p.baseProp.declaringType.draftClassName("$"),
                                p.baseProp.slotName
                            )
                            p = p.nextProp
                        }
                        unindent()
                        add("\n)")
                    }

                    val tailProp = prop.toTailProp()
                    val tailBaseProp = tailProp.baseProp
                    if (prop.isIdOnly) {
                        if (dtoType.modifiers.contains(DtoModifier.SPECIFICATION)) {
                            add(",\nnull")
                        } else {
                            add(
                                ",\n%T.%L(%T::class.java, ",
                                DTO_PROP_ACCESSOR,
                                if (tailBaseProp.isList) "idListGetter" else "idReferenceGetter",
                                tailBaseProp.targetTypeName(overrideNullable = false)
                            )
                            addConverterLoading(prop, false)
                            add(")")
                            add(
                                ",\n%T.%L(%T::class.java, ",
                                DTO_PROP_ACCESSOR,
                                if (tailBaseProp.isList) "idListSetter" else "idReferenceSetter",
                                tailBaseProp.targetTypeName(overrideNullable = false)
                            )
                            addConverterLoading(prop, false)
                            add(")")
                        }
                    } else if (tailProp.targetType != null) {
                        if (dtoType.modifiers.contains(DtoModifier.SPECIFICATION)) {
                            add(",\nnull")
                        } else {
                            add(
                                ",\n%T.%L<%T, %L> {",
                                DTO_PROP_ACCESSOR,
                                if (tailBaseProp.isList) "objectListGetter" else "objectReferenceGetter",
                                tailBaseProp.targetTypeName(overrideNullable = false),
                                propElementName(prop)
                            )
                            indent()
                            add("\n%L(it)", propElementName(prop))
                            unindent()
                            add("\n}")

                            add(
                                ",\n%T.%L<%T, %L> {",
                                DTO_PROP_ACCESSOR,
                                if (tailBaseProp.isList) "objectListSetter" else "objectReferenceSetter",
                                tailBaseProp.targetTypeName(overrideNullable = false),
                                propElementName(prop)
                            )
                            indent()
                            add(
                                "\nit.%L()",
                                if (tailBaseProp.targetType!!.isEntity) "toEntity" else "toImmutable"
                            )
                            unindent()
                            add("\n}")
                        }
                    } else if (prop.enumType !== null) {
                        val enumType = prop.enumType!!
                        val enumTypeName = tailBaseProp.targetTypeName(overrideNullable = false)
                        if (dtoType.modifiers.contains(DtoModifier.SPECIFICATION)) {
                            add(",\nnull")
                        } else {
                            add(",\n{\n")
                            indent()
                            beginControlFlow("when (it as %T)", enumTypeName)
                            for ((en, v) in enumType.valueMap) {
                                addStatement("%T.%L -> %L", enumTypeName, en, v)
                            }
                            endControlFlow()
                            unindent()
                            add("}")
                        }
                        add(",\n{\n")
                        indent()
                        addValueToEnum(prop)
                        unindent()
                        add("}")
                    } else if (prop.dtoConverterMetadata != null) {
                        add(",\n{ ")
                        addConverterLoading(prop, true)
                        add(".output(it) }")
                        add(",\n{ ")
                        addConverterLoading(prop, true)
                        add(".input(it) }")
                    }

                    unindent()
                    add("\n)")
                }
                .build()
        )
        addProperty(builder.build())
    }

    private fun TypeSpec.Builder.addSpecificationConverter(prop: DtoProp<ImmutableType, ImmutableProp>) {
        if (!isSpecificationConverterRequired(prop)) {
            return
        }
        val baseProp = prop.toTailProp().baseProp
        val baseTypeName = when (prop.funcName) {
            "id" -> baseProp.targetType!!.idProp!!.typeName().let {
                if (baseProp.isList && !dtoType.modifiers.contains(DtoModifier.SPECIFICATION)) {
                    LIST.parameterizedBy(it)
                } else {
                    it
                }
            }

            "valueIn", "valueNotIn" ->
                LIST.parameterizedBy(baseProp.typeName())

            "associatedIdEq", "associatedIdNe" ->
                baseProp.targetType!!.idProp!!.typeName()

            "associatedIdIn", "associatedIdNotIn" ->
                LIST.parameterizedBy(baseProp.targetType!!.idProp!!.typeName())

            else -> baseProp.typeName()
        }.copy(nullable = prop.isNullable)
        val builder = FunSpec
            .builder(StringUtil.identifier("_convert", prop.getName()))
            .addModifiers(KModifier.PUBLIC)
            .addParameter("value", propTypeName(prop))
            .returns(baseTypeName)
            .addCode(
                CodeBlock
                    .builder()
                    .apply {
                        if (prop.isNullable) {
                            beginControlFlow("if (value === null)")
                            addStatement("return null")
                            endControlFlow()
                        }
                        if (prop.enumType !== null) {
                            add("return ")
                            addValueToEnum(prop, "value")
                        } else {
                            add(
                                "return %T.%L.unwrap().%L<%T, %T>(%L).input(value)",
                                baseProp.declaringType.propsClassName,
                                StringUtil.snake(baseProp.name, SnakeCase.UPPER),
                                if (baseProp.isAssociation(true)) "getAssociatedIdConverter" else "getConverter",
                                baseTypeName,
                                propTypeName(prop).copy(nullable = false),
                                if (baseProp.isAssociation(true) || prop.isFunc("valueIn", "valueNotIn")) "true" else ""
                            )
                        }
                    }
                    .build()
            )
        addFunction(builder.build())
    }

    private fun TypeSpec.Builder.addHibernateValidatorEnhancement(getter: Boolean) {
        addFunction(
            FunSpec
                .builder(
                    "\$\$_hibernateValidator_get${
                        if (getter) "Getter" else "Field"
                    }Value"
                )
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("name", STRING)
                .returns(ANY.copy(nullable = true))
                .beginControlFlow("return when(name)")
                .apply {
                    for (prop in dtoType.props) {
                        addStatement(
                            "%S -> %L",
                            if (getter) {
                                StringUtil.identifier(
                                    if (propTypeName(prop) == BOOLEAN) "is" else "get",
                                    prop.name
                                )
                            } else {
                                prop.name
                            },
                            prop.name
                        )
                    }
                }
                .addStatement(
                    "else -> throw IllegalArgumentException(%L)",
                    "\"No ${if (getter) "getter" else "field"} named \\\"\${name}\\\"\""
                )
                .endControlFlow()
                .build()
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun propTypeName(prop: AbstractProp): TypeName =
        when (prop) {
            is DtoProp<*, *> -> propTypeName(prop as DtoProp<ImmutableType, ImmutableProp>)
            is UserProp -> typeName(prop.typeRef)
            else -> error("Internal bug")
        }

    private fun propTypeName(prop: DtoProp<ImmutableType, ImmutableProp>): TypeName {

        val baseProp = prop.toTailProp().baseProp
        val enumType = prop.enumType
        if (enumType !== null) {
            return (if (enumType.isNumeric) INT else STRING).copy(nullable = prop.isNullable)
        }

        val metadata = prop.dtoConverterMetadata
        val propElementName = propElementName(prop)
        if (dtoType.modifiers.contains(DtoModifier.SPECIFICATION)) {
            val funcName = prop.toTailProp().getFuncName()
            if (funcName != null) {
                when (funcName) {
                    "null", "notNull" ->
                        return BOOLEAN.copy(nullable = prop.isNullable)

                    "valueIn", "valueNotIn" ->
                        return COLLECTION.parameterizedBy(
                            metadata?.targetTypeName ?: propElementName.toList(baseProp.isList)
                        ).copy(nullable = prop.isNullable)

                    "id", "associatedIdEq", "associatedIdNe" ->
                        return baseProp.targetType!!.idProp!!.clientClassName.copy(nullable = prop.isNullable)

                    "associatedIdIn", "associatedIdNotIn" ->
                        return COLLECTION.parameterizedBy(baseProp.targetType!!.idProp!!.clientClassName)
                            .copy(nullable = prop.isNullable)
                }
            }
            if (baseProp.isAssociation(true)) {
                return propElementName.copy(nullable = prop.isNullable)
            }
        }
        if (metadata != null) {
            return metadata.targetTypeName.copy(nullable = prop.isNullable)
        }

        return propElementName
            .toList(baseProp.isList && !(propElementName is ParameterizedTypeName && propElementName.rawType == LIST))
            .copy(nullable = prop.isNullable)
    }

    private fun propElementName(prop: DtoProp<ImmutableType, ImmutableProp>): TypeName {
        val tailProp = prop.toTailProp()
        val targetType = tailProp.targetType
        if (targetType !== null) {
            if (tailProp.isRecursive && !targetType.isFocusedRecursion) {
                return getDtoClassName()
            }
            if (targetType.name === null) {
                val list: MutableList<String> = ArrayList()
                collectNames(list)
                if (!prop.isRecursive || targetType.isFocusedRecursion) {
                    list.add(targetSimpleName(tailProp))
                }
                return ClassName(
                    root.dtoType.packageName,
                    list[0],
                    *list.subList(1, list.size).toTypedArray()
                )
            }
            return ClassName(
                root.dtoType.packageName,
                targetType.name!!
            )
        }
        val baseProp = tailProp.baseProp
        return if (tailProp.isIdOnly) {
            baseProp.targetType!!.idProp!!.clientClassName
        } else if (baseProp.idViewBaseProp !== null) {
            baseProp.idViewBaseProp!!.targetType!!.idProp!!.clientClassName
        } else {
            tailProp.baseProp.clientClassName
        }.copy(nullable = false)
    }

    private fun collectNames(list: MutableList<String>) {
        if (parent == null) {
            list.add(dtoType.name!!)
        } else if (innerClassName !== null) {
            parent.collectNames(list)
            list.add(innerClassName)
        }
    }

    private fun targetSimpleName(prop: DtoProp<ImmutableType, ImmutableProp>): String {
        val targetType = prop.targetType ?: throw IllegalArgumentException("prop is not association")
        if (prop.isRecursive && !targetType.isFocusedRecursion) {
            return innerClassName ?: dtoType.name ?: error("Internal bug: No target simple name")
        }
        return standardTargetSimpleName("TargetOf_${prop.name}")
    }

    private fun standardTargetSimpleName(targetSimpleName: String): String {
        var conflict = false
        var generator: DtoGenerator? = this
        while (generator != null) {
            if ((generator.innerClassName ?: generator.dtoType.name) == targetSimpleName) {
                conflict = true
                break
            }
            generator = generator.parent
        }
        if (!conflict) {
            return targetSimpleName
        }
        for (i in 2..99) {
            conflict = false
            val newTargetSimpleName = targetSimpleName + '_' + i
            generator = this
            while (generator != null) {
                if ((generator.innerClassName ?: generator.dtoType.name) == newTargetSimpleName) {
                    conflict = true
                    break
                }
                generator = generator.parent
            }
            if (!conflict) {
                return newTargetSimpleName
            }
        }
        throw AssertionError("Dto is too deep")
    }

    private fun CodeBlock.Builder.addValueToEnum(prop: DtoProp<ImmutableType, ImmutableProp>, variableName: String = "it") {
        beginControlFlow(
            "when ($variableName as %T)",
            if (propTypeName(prop).copy(nullable = false) == INT) INT else STRING
        )
        val enumTypeName = prop.toTailProp().baseProp.typeName(overrideNullable = false)
        for ((v, en) in prop.enumType!!.constantMap) {
            addStatement("%L -> %T.%L", v, enumTypeName, en)
        }
        addStatement("else -> throw IllegalArgumentException(")
        indent()
        addStatement("%S + $variableName + %S", "Illegal value \"", "\" for the enum type \"$enumTypeName\"")
        unindent()
        add(")\n")
        endControlFlow()
    }

    private fun CodeBlock.Builder.addConverterLoading(
        prop: DtoProp<ImmutableType, ImmutableProp>,
        forList: Boolean,
    ) {
        val baseProp: ImmutableProp = prop.toTailProp().getBaseProp()
        add(
            "%T.%L.unwrap().%L",
            baseProp.declaringType.propsClassName,
            StringUtil.snake(baseProp.name, SnakeCase.UPPER),
            if (prop.toTailProp().getBaseProp()
                    .isAssociation(true)
            ) {
                "getAssociatedIdConverter<Any, Any>($forList)"
            } else {
                "getConverter<Any, Any>()"
            }
        )
    }

    private fun isSpecificationConverterRequired(prop: DtoProp<ImmutableType, ImmutableProp>): Boolean {
        return if (!dtoType.modifiers.contains(DtoModifier.SPECIFICATION)) {
            false
        } else {
            prop.getEnumType() != null || prop.dtoConverterMetadata != null
        }
    }

    private val DtoProp<ImmutableType, ImmutableProp>.dtoConverterMetadata: ConverterMetadata?
        get() {
            val baseProp = toTailProp().getBaseProp()
            val resolver = baseProp.ctx.resolver
            val metadata = baseProp.converterMetadata
            if (metadata != null) {
                return metadata
            }
            val funcName = getFuncName()
            if ("id" == funcName) {
                val metadata = baseProp.targetType!!.idProp!!.converterMetadata
                if (metadata != null && baseProp.isList && !dtoType.modifiers.contains(DtoModifier.SPECIFICATION)) {
                    return metadata.toListMetadata(resolver)
                }
                return metadata
            }
            if ("associatedInEq" == funcName || "associatedInNe" == funcName) {
                return baseProp.targetType!!.idProp!!.converterMetadata
            }
            if ("associatedIdIn" == funcName || "associatedIdNotIn" == funcName) {
                return baseProp.targetType!!.idProp!!.converterMetadata?.toListMetadata(resolver)
            }
            if (baseProp.idViewBaseProp !== null) {
                return baseProp.idViewBaseProp!!.targetType!!.idProp!!.converterMetadata?.let {
                    if (baseProp.isList) it.toListMetadata(resolver) else it
                }
            }
            return null
        }

    private fun allowedTargets(typeName: String): Set<AnnotationUseSiteTarget> =
        useSiteTargetMap.computeIfAbsent(typeName) { tn ->
            val annotation = ctx.resolver.getClassDeclarationByName(tn)
                ?: error("Internal bug, cannot resolve annotation type \"$typeName\"")
            var field = false
            var getter = false
            var setter = false
            var property = false
            annotation.annotation(kotlin.annotation.Target::class)?.let {
                it
                    .get<List<Any>>("allowedTargets")
                    ?.forEach {
                        val s = it.toString()
                        when {
                            s.endsWith("FIELD") ->
                                field = true

                            s.endsWith("PROPERTY_GETTER") ->
                                getter = true

                            s.endsWith("FUNCTION") ->
                                getter = true

                            s.endsWith("PROPERTY_SETTER") ->
                                setter = true

                            s.endsWith("PROPERTY") ->
                                property = true
                        }
                    }
            }
            annotation.annotation(java.lang.annotation.Target::class)
                ?.get<List<java.lang.annotation.ElementType>>("value")
                ?.forEach {
                    val s = it.toString()
                    when {
                        s.endsWith("FIELD") ->
                            field = true

                        s.endsWith("METHOD") ->
                            getter = true
                    }
                }
            val targets = mutableSetOf<AnnotationUseSiteTarget>()
            if (field) {
                targets += AnnotationUseSiteTarget.FIELD
            }
            if (getter) {
                targets += AnnotationUseSiteTarget.GET
            }
            if (setter) {
                targets += AnnotationUseSiteTarget.SET
            }
            if (property) {
                targets += AnnotationUseSiteTarget.PROPERTY
            }
            targets
        }

    private fun TypeSpec.Builder.addCopy() {
        addFunction(
            FunSpec
                .builder("copy")
                .returns(getDtoClassName())
                .apply {
                    val args = mutableListOf<String>()
                    for (dtoProp in dtoType.dtoProps) {
                        addParameter(
                            ParameterSpec.builder(dtoProp.name, propTypeName(dtoProp))
                                .defaultValue("this.${dtoProp.name}")
                                .build()
                        )
                        args += dtoProp.name
                        statePropName(dtoProp, false)?.let {
                            addParameter(
                                ParameterSpec.builder(it, BOOLEAN)
                                    .defaultValue("this.$it")
                                    .build()
                            )
                            args += it
                        }
                    }
                    for (userProp in dtoType.userProps) {
                        addParameter(
                            ParameterSpec.builder(userProp.alias, typeName(userProp.typeRef))
                                .defaultValue("this.${userProp.alias}")
                                .build()
                        )
                        args += userProp.alias
                    }
                    addStatement("return %T(%L)", getDtoClassName(), args.joinToString())
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addHashCode() {
        addFunction(
            FunSpec
                .builder("hashCode")
                .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                .returns(INT)
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            dtoType.props.forEachIndexed { index, prop ->
                                val hashCodeFunName = if (propTypeName(prop).isArray()) {
                                    "contentHashCode"
                                } else {
                                    "hashCode"
                                }
                                addStatement(
                                    "%L %L",
                                    if (index == 0) "var _hash =" else "_hash = 31 * _hash +",
                                    if (prop.isNullable) {
                                        "(${prop.alias}?.$hashCodeFunName() ?: 0)"
                                    } else {
                                        "${prop.alias}.$hashCodeFunName()"
                                    }
                                )
                                statePropName(prop, false)?.let {
                                    addStatement("_hash = _hash * 31 + %L.hashCode()", it)
                                }
                            }
                            addStatement("return _hash")
                        }
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addEquals() {
        addFunction(
            FunSpec
                .builder("equals")
                .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                .addParameter("other", ANY.copy(nullable = true))
                .returns(BOOLEAN)
                .addCode(
                    CodeBlock.builder()
                        .apply {
                            addStatement("val _other = other as? %T ?: return false", getDtoClassName())
                            dtoType.props.forEachIndexed { index, prop ->
                                if (index == 0) {
                                    add("return ")
                                }
                                val statePropName = statePropName(prop, false)
                                if (statePropName !== null) {
                                    add("%L == _other.%L && (\n", statePropName, statePropName)
                                    indent()
                                    add("!%L || ", statePropName)
                                }
                                if (propTypeName(prop).isArray()) {
                                    add("%L.contentEquals(_other.%L)", prop.alias, prop.alias)
                                } else {
                                    add("%L == _other.%L", prop.alias, prop.alias)
                                }
                                if (statePropName !== null) {
                                    unindent()
                                    add("\n)")
                                }
                                if (index + 1 < dtoType.props.size) {
                                    add(" &&")
                                }
                                add("\n")
                            }
                        }
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addToString() {
        addFunction(
            FunSpec.builder("toString")
                .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                .returns(STRING)
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            val hashCondProps = dtoType.modifiers.contains(DtoModifier.INPUT) &&
                                    dtoType.dtoProps.any { statePropName(it, false) != null || it.inputModifier == DtoModifier.FUZZY }
                            if (hashCondProps) {
                                addStatement("val builder = StringBuilder()")
                                addStatement("var separator = \"\"")
                                addStatement("builder.append(%S).append('(')", simpleNamePart())
                                for (prop in dtoType.getDtoProps()) {
                                    val stateFieldName = statePropName(prop, false)
                                    if (stateFieldName != null) {
                                        beginControlFlow("if (%L)", stateFieldName)
                                    } else if (prop.getInputModifier() == DtoModifier.FUZZY) {
                                        beginControlFlow("if (%L != null)", prop.getName())
                                    }
                                    if (prop.getName() == "builder") {
                                        addStatement(
                                            "builder.append(separator).append(%S).append(this.%L)",
                                            prop.getName() + '=',
                                            prop.getName()
                                        )
                                        addStatement("separator = \", \"")
                                    } else {
                                        addStatement(
                                            "builder.append(separator).append(%S).append(%L)",
                                            prop.getName() + '=',
                                            prop.getName()
                                        )
                                        addStatement("separator = \", \"")
                                    }
                                    if (stateFieldName != null || prop.getInputModifier() == DtoModifier.FUZZY) {
                                        endControlFlow()
                                    }
                                }
                                for (prop in dtoType.getUserProps()) {
                                    if (prop.alias == "builder") {
                                        addStatement(
                                            "builder.append(separator).append(%S).append(this.%L)",
                                            prop.alias + '=',
                                            prop.alias
                                        )
                                    } else {
                                        addStatement(
                                            "builder.append(separator).append(%S).append(%L)",
                                            prop.alias + '=',
                                            prop.alias
                                        )
                                    }
                                    addStatement("separator = \", \"")
                                }
                                addStatement("builder.append(')')")
                                addStatement("return builder.toString()")
                            } else {
                                add("return %S +\n", simpleNamePart() + "(")
                                dtoType.props.forEachIndexed { index, prop ->
                                    add(
                                        "    %S + %L + \n",
                                        (if (index == 0) "" else ", ") + prop.name + '=',
                                        prop.name
                                    )
                                }
                                add("    %S\n", ")")
                            }
                        }
                        .build()
                )
                .build()
        )
    }

    private fun simpleNamePart(): String =
        (innerClassName ?: dtoType.name!!).let { name ->
            parent
                ?.let { "${it.simpleNamePart()}.$name" }
                ?: name
        }

    private inner class Document {

        private val dtoTypeDoc: Doc? by lazy {
            Doc.parse(dtoType.doc)
        }

        private val baseTypeDoc: Doc? by lazy {
            Doc.parse(baseDocString)
        }

        val value: String? by lazy {
            (dtoTypeDoc?.toString() ?: baseTypeDoc?.toString())?.replace("%", "%%")
        }

        operator fun get(prop: AbstractProp): String? {
            return getImpl(prop)?.let {
                it.replace("%", "%%")
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun getImpl(prop: AbstractProp): String? {
            val baseProp = (prop as? DtoProp<*, ImmutableProp?>)?.toTailProp()?.getBaseProp()
            if (prop.doc !== null) {
                val doc = Doc.parse(prop.doc)
                if (doc != null) {
                    return doc.toString()
                }
            }
            val dtoTypeDoc = this.dtoTypeDoc
            if (dtoTypeDoc != null) {
                val name = prop.getAlias() ?: baseProp!!.name
                val doc = dtoTypeDoc.parameterValueMap[name]
                if (doc != null) {
                    return doc
                }
            }
            if (baseProp != null) {
                val doc = Doc.parse(baseDocString(baseProp))
                if (doc != null) {
                    return doc.toString()
                }
            }
            val baseTypeDoc = this.baseTypeDoc
            if (baseTypeDoc != null && baseProp != null) {
                val doc = baseTypeDoc.parameterValueMap[baseProp.name]
                if (doc != null) {
                    return doc
                }
            }
            return null
        }
    }

    private val isImpl: Boolean
        get() = dtoType.baseType.isEntity || !dtoType.modifiers.contains(DtoModifier.SPECIFICATION)

    internal fun statePropName(prop: AbstractProp, builder: Boolean): String? =
        when {
            !prop.isNullable -> null
            prop !is DtoProp<*, *> -> null
            !dtoType.modifiers.contains(DtoModifier.INPUT) -> null
            else -> prop.inputModifier?.takeIf {
                (it == DtoModifier.FIXED && builder) || it == DtoModifier.DYNAMIC
            }?.let {
                StringUtil.identifier("is", prop.name, "Loaded")
            }
        }

    private val isSerializerRequired: Boolean by lazy {
        dtoType.modifiers.contains(DtoModifier.INPUT) &&
                dtoType.dtoProps.any { it.inputModifier == DtoModifier.DYNAMIC }
    }

    private val isBuilderRequired: Boolean by lazy {
        dtoType.modifiers.contains(DtoModifier.INPUT) &&
                dtoType.dtoProps.any { prop ->
                    prop.inputModifier.let { it == DtoModifier.FIXED || it == DtoModifier.DYNAMIC }
                }
    }

    private val isHibernateValidatorEnhancementRequired: Boolean by lazy {
        ctx.isHibernateValidatorEnhancement &&
                dtoType.dtoProps.any { it.inputModifier == DtoModifier.DYNAMIC }
    }

    private val baseDocString: String?
        get() = docMetadata.getString(dtoType.baseType.classDeclaration)

    private fun baseDocString(prop: ImmutableProp): String? =
        docMetadata.getString(prop.propDeclaration)

    companion object {

        @JvmStatic
        private val NEW = MemberName("org.babyfish.jimmer.kt", "new")

        @JvmStatic
        private val JSON_DESERIALIZE_TYPE_NAME = JsonDeserialize::class.qualifiedName!!

        @JvmStatic
        private val JSON_PROPERTY_TYPE_NAME = JsonProperty::class.qualifiedName!!

        @JvmStatic
        private val KOTLIN_DTO_TYPE_NAME = "org.babyfish.jimmer.kt.dto.KotlinDto"

        private fun isCopyableAnnotation(annotation: KSAnnotation, dtoAnnotations: Collection<Anno>): Boolean {
            val qualifiedName =
                annotation.annotationType.fastResolve().declaration.qualifiedName?.asString()
                    ?: throw DtoException(
                        """
                        Unable to resolve qualifiedName for annotation: '${annotation.annotationType.fastResolve()}'
                        Possible reasons:
                        1. The annotation's dependency is missing from compilation classpath
                        2. Required library is not included as a dependency
                        3. Dependency is declared with 'implementation' instead of 'api' configuration
                        
                        Solution: Add the corresponding dependency to your build configuration.
                        """.trimIndent()
                    )
            return (
                    qualifiedName != KOTLIN_DTO_TYPE_NAME && (
                            !qualifiedName.startsWith("org.babyfish.jimmer.") ||
                                    qualifiedName.startsWith("org.babyfish.jimmer.client.")
                            ) && dtoAnnotations.none {
                        it.qualifiedName == annotation.annotationType.fastResolve().declaration.qualifiedName?.asString()
                    }
                    )
        }

        internal fun annotationOf(anno: Anno, target: AnnotationSpec.UseSiteTarget? = null): AnnotationSpec =
            AnnotationSpec
                .builder(ClassName.bestGuess(anno.qualifiedName))
                .apply {
                    if (anno.valueMap.isNotEmpty()) {
                        addMember(
                            CodeBlock
                                .builder()
                                .apply {
                                    if (anno.valueMap.let { it.size == 1 && it.keys.first() == "value" }) {
                                        add("(")
                                        add(anno.valueMap.values.first())
                                        add(")")
                                    } else {
                                        add("\n")
                                        add(anno.valueMap)
                                        add("\n")
                                    }
                                }
                                .build()
                        )
                    }
                    target?.let {
                        useSiteTarget(it)
                    }
                }
                .build()

        private fun CodeBlock.Builder.add(value: Value) {
            when (value) {
                is ArrayValue -> {
                    add("[\n")
                    indent()
                    var addSeparator = false
                    for (element in value.elements) {
                        if (addSeparator) {
                            add(", \n")
                        } else {
                            addSeparator = true
                        }
                        add(element)
                    }
                    unindent()
                    add("\n]")
                }

                is AnnoValue -> {
                    add("%T", ClassName.bestGuess(value.anno.qualifiedName))
                    if (value.anno.valueMap.isEmpty()) {
                        add("{}")
                    } else if (value.anno.valueMap.let { it.size == 1 && it.keys.first() == "value" }) {
                        add("(")
                        add(value.anno.valueMap.values.first())
                        add(")")
                    } else {
                        add("(\n")
                        add(value.anno.valueMap)
                        add("\n)")
                    }
                }

                is TypeRefValue -> value.typeRef.let {
                    if (it.isNullable) {
                        add(
                            "java.lang.%L::class",
                            when (it.typeName) {
                                "Char" -> "Character"
                                "Int" -> "Integer"
                                else -> it.typeName
                            }
                        )
                    } else {
                        add("%T::class", typeName(it))
                    }
                }

                is EnumValue -> add(
                    "%T.%N",
                    ClassName.bestGuess(value.qualifiedName),
                    value.constant
                )

                else -> add((value as LiteralValue).value.replace("%", "%%"))
            }
        }

        private fun CodeBlock.Builder.add(valueMap: Map<String, Value>) {
            indent()
            var addSeparator = false
            for ((name, value) in valueMap) {
                if (addSeparator) {
                    add(", \n")
                } else {
                    addSeparator = true
                }
                add("%N = ", name)
                add(value)
            }
            unindent()
        }

        fun typeName(typeRef: TypeRef?): TypeName {
            val typeName = if (typeRef === null) {
                STAR
            } else {
                when (typeRef.typeName) {
                    TypeRef.TN_BOOLEAN -> BOOLEAN
                    TypeRef.TN_CHAR -> CHAR
                    TypeRef.TN_BYTE -> BYTE
                    TypeRef.TN_SHORT -> SHORT
                    TypeRef.TN_INT -> INT
                    TypeRef.TN_LONG -> LONG
                    TypeRef.TN_FLOAT -> FLOAT
                    TypeRef.TN_DOUBLE -> DOUBLE
                    TypeRef.TN_ANY -> ANY
                    TypeRef.TN_STRING -> STRING
                    TypeRef.TN_ARRAY ->
                        if (typeRef.arguments[0].typeRef == null) {
                            ARRAY.parameterizedBy(STAR)
                        } else if (typeRef.arguments[0].typeRef?.isNullable == true) {
                            ARRAY.parameterizedBy(typeName(typeRef.arguments[0].typeRef))
                        } else {
                            val componentTypeRef = typeRef.arguments[0].typeRef
                            if (componentTypeRef == null) {
                                ARRAY.parameterizedBy(
                                    WildcardTypeName.producerOf(ANY)
                                )
                            } else {
                                when (componentTypeRef.typeName) {
                                    TypeRef.TN_BOOLEAN -> BOOLEAN_ARRAY
                                    TypeRef.TN_CHAR -> CHAR_ARRAY
                                    TypeRef.TN_BYTE -> BYTE_ARRAY
                                    TypeRef.TN_SHORT -> SHORT_ARRAY
                                    TypeRef.TN_INT -> INT_ARRAY
                                    TypeRef.TN_LONG -> LONG_ARRAY
                                    TypeRef.TN_FLOAT -> FLOAT_ARRAY
                                    TypeRef.TN_DOUBLE -> DOUBLE_ARRAY
                                    else -> ARRAY.parameterizedBy(typeName(typeRef.arguments[0].typeRef))
                                }
                            }
                        }

                    TypeRef.TN_ITERABLE -> ITERABLE
                    TypeRef.TN_MUTABLE_ITERABLE -> MUTABLE_ITERABLE
                    TypeRef.TN_COLLECTION -> COLLECTION
                    TypeRef.TN_MUTABLE_COLLECTION -> MUTABLE_COLLECTION
                    TypeRef.TN_LIST -> LIST
                    TypeRef.TN_MUTABLE_LIST -> MUTABLE_LIST
                    TypeRef.TN_SET -> SET
                    TypeRef.TN_MUTABLE_SET -> MUTABLE_SET
                    TypeRef.TN_MAP -> MAP
                    TypeRef.TN_MUTABLE_MAP -> MUTABLE_MAP
                    else -> ClassName.bestGuess(typeRef.typeName)
                }
            }
            val args = typeRef
                ?.arguments
                ?.takeIf { it.isNotEmpty() && typeRef.typeName != TypeRef.TN_ARRAY }
                ?.let { args ->
                    Array(args.size) { i ->
                        typeName(args[i].typeRef).let {
                            when {
                                args[i].isIn -> WildcardTypeName.consumerOf(it)
                                args[i].isOut -> WildcardTypeName.producerOf(it)
                                else -> it
                            }
                        }
                    }
                }
            return if (args == null) {
                typeName
            } else {
                (typeName as ClassName).parameterizedBy(*args)
            }.copy(
                nullable = typeRef?.isNullable ?: false
            )
        }

        private fun defaultValue(prop: UserProp): String? {
            val typeRef = prop.typeRef
            return if (prop.defaultValueText !== null) {
                prop.defaultValueText!!
            } else if (typeRef.isNullable) {
                "null"
            } else {
                when (typeRef.typeName) {
                    TypeRef.TN_BOOLEAN -> "false"
                    TypeRef.TN_CHAR -> "'\\0'"

                    TypeRef.TN_BYTE, TypeRef.TN_SHORT, TypeRef.TN_INT -> "0"
                    TypeRef.TN_LONG -> "0L"
                    TypeRef.TN_FLOAT -> "0F"
                    TypeRef.TN_DOUBLE -> "0.0"

                    TypeRef.TN_STRING -> "\"\""

                    TypeRef.TN_ARRAY -> if (typeRef.arguments[0].typeRef == null) {
                        "emptyArray<Any?>()"
                    } else if (typeRef.arguments[0].typeRef?.isNullable == true) {
                        "emptyArray()"
                    } else {
                        val componentTypeRef = typeRef.arguments[0].typeRef
                        if (componentTypeRef === null) {
                            "emptyArray()"
                        } else {
                            when (componentTypeRef.typeName) {
                                TypeRef.TN_BOOLEAN -> "booleanArrayOf()"
                                TypeRef.TN_CHAR -> "charArrayOf()"
                                TypeRef.TN_BYTE -> "byteArrayOf()"
                                TypeRef.TN_SHORT -> "shortArrayOf()"
                                TypeRef.TN_INT -> "intArrayOf()"
                                TypeRef.TN_LONG -> "longArrayOf()"
                                TypeRef.TN_FLOAT -> "floatArrayOf()"
                                TypeRef.TN_DOUBLE -> "doubleArrayOf()"
                                else -> "emptyArray()"
                            }
                        }
                    }

                    TypeRef.TN_ITERABLE, TypeRef.TN_COLLECTION, TypeRef.TN_LIST ->
                        if (typeRef.arguments[0].typeRef === null) {
                            "emptyList<Any?>()"
                        } else {
                            "emptyList()"
                        }

                    TypeRef.TN_MUTABLE_ITERABLE, TypeRef.TN_MUTABLE_COLLECTION, TypeRef.TN_MUTABLE_LIST ->
                        if (typeRef.arguments[0].typeRef === null) {
                            "mutableListOf<Any?>()"
                        } else {
                            "mutableListOf()"
                        }

                    TypeRef.TN_SET -> "emptySet()"
                    TypeRef.TN_MUTABLE_SET -> "mutableSetOf()"

                    TypeRef.TN_MAP -> "emptyMap()"
                    TypeRef.TN_MUTABLE_MAP -> "mutableMapOf()"

                    else -> null
                }
            }
        }

        private fun String.simpleName() =
            lastIndexOf('.').let {
                if (it == -1) {
                    this
                } else {
                    substring(it + 1)
                }
            }

        private fun TypeName.toList(isList: Boolean) =
            if (isList) {
                LIST.parameterizedBy(this.copy(nullable = false))
            } else {
                this
            }

        private fun TypeName.isArray(): Boolean =
            if (this is ClassName) {
                when (this.reflectionName()) {
                    "kotlin.BooleanArray", "kotlin.CharArray",
                    "kotlin.ByteArray", "kotlin.ShortArray", "kotlin.IntArray", "kotlin.LongArray",
                    "kotlin.FloatArray", "kotlin.DoubleArray",
                    "kotlin.Array",
                        -> true

                    else -> false
                }
            } else if (this is ParameterizedTypeName) {
                this.rawType.isArray()
            } else {
                false
            }

        val DOC_EXPLICIT_FUN = "Avoid anonymous lambda affects coverage of non-kotlin-friendly tools such as jacoco"

        private val EXPRESSION_PACKAGE = "org.babyfish.jimmer.sql.kt.ast.expression"

        // Issue#1218, Avoid bug of kotlinpoet
        private fun standardSpec(spec: AnnotationSpec): AnnotationSpec {
            var modifiedIndex = -1
            for (i in spec.members.indices) {
                val text = spec.members[i].toString()
                val eqIndex = text.indexOf('=')
                val modified = if (eqIndex == -1) {
                    true
                } else {
                    val quoteIndex = text.indexOf('"')
                    quoteIndex != -1 && quoteIndex < eqIndex
                }
                if (modified) {
                    if (modifiedIndex != -1) {
                        return spec
                    }
                    modifiedIndex = i
                }
            }
            if (modifiedIndex == -1) {
                return spec
            }
            val builder = spec.toBuilder()
            builder.members[modifiedIndex] = CodeBlock
                .builder()
                .add("value = ")
                .add(builder.members[modifiedIndex])
                .build()
            return builder.build()
        }
    }
}