package org.babyfish.jimmer.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import org.babyfish.jimmer.dto.compiler.*
import org.babyfish.jimmer.dto.compiler.Anno.*
import org.babyfish.jimmer.impl.util.StringUtil
import org.babyfish.jimmer.impl.util.StringUtil.SnakeCase
import org.babyfish.jimmer.ksp.annotation
import org.babyfish.jimmer.ksp.get
import org.babyfish.jimmer.ksp.meta.*
import java.io.OutputStreamWriter
import java.util.*
import kotlin.math.min

class DtoGenerator private constructor(
    private val dtoType: DtoType<ImmutableType, ImmutableProp>,
    private val mutable: Boolean,
    private val codeGenerator: CodeGenerator?,
    private val parent: DtoGenerator?,
    private val innerClassName: String?
) {
    private val root: DtoGenerator = parent?.root ?: this

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
        dtoType: DtoType<ImmutableType, ImmutableProp>,
        mutable: Boolean,
        codeGenerator: CodeGenerator?,
    ): this(dtoType, mutable, codeGenerator, null, null)

    val typeBuilder: TypeSpec.Builder
        get() = _typeBuilder ?: error("Type builder is not ready")

    private fun getDtoClassName(vararg nestedNames: String): ClassName {
            if (innerClassName !== null) {
                val list: MutableList<String> = ArrayList()
                collectNames(list)
                list.addAll(nestedNames.toList())
                return ClassName(
                    root.packageName(),
                    list[0],
                    *list.subList(1, list.size).toTypedArray()
                )
            }
            return ClassName(
                root.packageName(),
                dtoType.name!!,
                *nestedNames
            )
        }

    private fun packageName() =
        dtoType
            .baseType
            .className
            .packageName
            .takeIf { it.isNotEmpty() }
            ?.let { "$it.dto" }
            ?: "dto"

    fun generate(allFiles: List<KSFile>) {
        if (codeGenerator != null) {
            codeGenerator.createNewFile(
                Dependencies(false, *allFiles.toTypedArray()),
                packageName(),
                dtoType.name!!
            ).use {
                val fileSpec = FileSpec
                    .builder(
                        packageName(),
                        dtoType.name!!
                    ).apply {
                        indent("    ")
                        addImports()
                        val builder = TypeSpec
                            .classBuilder(dtoType.name!!)
                            .addModifiers(KModifier.DATA)
                            .apply {
                                dtoType.path?.let { path ->
                                    addAnnotation(
                                        AnnotationSpec
                                            .builder(GENERATED_BY_CLASS_NAME)
                                            .addMember("file = %S", path)
                                            .build()
                                    )
                                }
                            }
                        for (anno in dtoType.annotations) {
                            builder.addAnnotation(annotationOf(anno))
                        }
                        _typeBuilder = builder
                        try {
                            addMembers(allFiles)
                            addType(builder.build())
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
                .apply {
                    if (dtoType.dtoProps.isNotEmpty()) {
                        addModifiers(KModifier.DATA)
                    }
                }
                for (anno in dtoType.annotations) {
                    builder.addAnnotation(annotationOf(anno))
                }
            _typeBuilder = builder
            try {
                addMembers(allFiles)
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
        packages: SortedSet<String>
    ) {
        packages += dtoType.baseType.className.packageName
        for (prop in dtoType.dtoProps) {
            val targetType = prop.targetType?.takeIf { dtoType !== it }
            if (targetType !== null) {
                collectImports(targetType, packages)
            } else {
                prop.baseProp.targetType?.className?.packageName?.let {
                    packages += it
                }
            }
        }
    }

    private fun addMembers(allFiles: List<KSFile>) {

        val isSpecification = dtoType.modifiers.contains(DtoTypeModifier.SPECIFICATION)
        typeBuilder.addSuperinterface(
            when {
                isSpecification ->
                    K_SPECIFICATION_CLASS_NAME
                dtoType.modifiers.contains(DtoTypeModifier.INPUT) ->
                    VIEWABLE_INPUT_CLASS_NAME
                else ->
                    VIEW_CLASS_NAME
            }.parameterizedBy(
                dtoType.baseType.className
            )
        )

        addPrimaryConstructor()
        if (!isSpecification) {
            addConverterConstructor()
        }

        if (isSpecification) {
            addApplyTo()
        } else {
            addToEntity()
        }

        if (!isSpecification) {
            typeBuilder.addType(
                TypeSpec
                    .companionObjectBuilder()
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
            val targetType = prop.targetType
            if (targetType != null && prop.isNewTarget) {
                DtoGenerator(
                    targetType,
                    mutable,
                    null,
                    this,
                    targetSimpleName(prop)
                ).generate(allFiles)
            }
        }
    }

    private fun TypeSpec.Builder.addMetadata() {
        addProperty(
            PropertySpec
                .builder(
                    "METADATA",
                    VIEW_METADATA_CLASS_NAME.parameterizedBy(
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
                                VIEW_METADATA_CLASS_NAME,
                                dtoType.baseType.className, getDtoClassName()
                            )
                            indent()
                            add("%M(%T::class).by", NEW_FETCHER, dtoType.baseType.className)
                            beginControlFlow("")
                            for (prop in dtoType.dtoProps) {
                                if (prop.nextProp === null) {
                                    addFetcherField(prop)
                                }
                            }
                            for (hiddenFlatProp in dtoType.hiddenFlatProps) {
                                addHiddenFetcherField(hiddenFlatProp)
                            }
                            endControlFlow()
                            unindent()
                            add(")")
                            beginControlFlow("")
                            addStatement("%T(it)", getDtoClassName())
                            endControlFlow()
                            unindent()
                        }
                        .build()
                )
                .build()
        )
    }

    private fun CodeBlock.Builder.addFetcherField(prop: DtoProp<ImmutableType, ImmutableProp>) {
        if (!prop.baseProp.isId) {
            if (prop.targetType !== null) {
                if (prop.isNewTarget) {
                    add(
                        "%N(%T.METADATA.fetcher)",
                        prop.baseProp.name,
                        propElementName(prop)
                    )
                    if (prop.isRecursive) {
                        beginControlFlow("")
                        addStatement("recursive()")
                        endControlFlow()
                    } else {
                        add("\n")
                    }
                }
            } else {
                addStatement("%N()", prop.baseProp.name)
            }
        }
    }

    private fun CodeBlock.Builder.addHiddenFetcherField(prop: DtoProp<ImmutableType, ImmutableProp>) {
        if ("flat" != prop.getFuncName()) {
            addFetcherField(prop)
            return
        }
        val targetDtoType = prop.getTargetType()!!
        beginControlFlow("%N", prop.getBaseProp().name)
        for (childProp in targetDtoType.dtoProps) {
            addHiddenFetcherField(childProp)
        }
        endControlFlow()
    }

    private fun addPrimaryConstructor() {
        val builder = FunSpec.constructorBuilder()
        builder.addPrimaryParameters()
        typeBuilder.primaryConstructor(builder.build())

        for (prop in dtoType.dtoProps) {
            typeBuilder.addProperty(
                PropertySpec
                    .builder(prop.name, propTypeName(prop))
                    .mutable(mutable)
                    .initializer(prop.name)
                    .apply {
                        if (!prop.isNullable) {
                            addAnnotation(
                                AnnotationSpec
                                    .builder(JSON_PROPERTY_CLASS_NAME)
                                    .addMember("required = true")
                                    .build()
                            )
                        }
                        if (prop.annotations.isEmpty()) {
                            for (anno in prop.baseProp.annotations { isCopyableAnnotation(it) }) {
                                addAnnotation(
                                    object : KSAnnotation by anno {
                                        override val useSiteTarget: AnnotationUseSiteTarget?
                                            get() = AnnotationUseSiteTarget.FIELD
                                    }.toAnnotationSpec()
                                )
                            }
                        } else {
                            for (anno in prop.annotations) {
                                addAnnotation(annotationOf(anno, AnnotationSpec.UseSiteTarget.FIELD))
                            }
                        }
                    }
                    .build()
            )
        }
        for (prop in dtoType.userProps) {
            typeBuilder.addProperty(
                PropertySpec
                    .builder(prop.alias, typeName(prop.typeRef))
                    .mutable(mutable)
                    .initializer(prop.alias)
                    .apply {
                        if (!prop.typeRef.isNullable) {
                            addAnnotation(
                                AnnotationSpec
                                    .builder(JSON_PROPERTY_CLASS_NAME)
                                    .addMember("required = true")
                                    .build()
                            )
                        }
                        for (anno in prop.annotations) {
                            addAnnotation(annotationOf(anno))
                        }
                    }
                    .build()
            )
        }
    }

    private fun FunSpec.Builder.addPrimaryParameters() {
        for (prop in dtoType.dtoProps) {
            addParameter(
                ParameterSpec
                    .builder(prop.name, propTypeName(prop))
                    .apply {
                        when {
                            prop.isNullable -> defaultValue("null")
                            prop.toTailProp().baseProp.isList -> defaultValue("emptyList()")
                        }
                    }
                    .build()
            )
        }
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
                .apply {
                    callThisConstructor(dtoType.props.map { prop ->
                        CodeBlock
                            .builder()
                            .indent()
                            .add("\n")
                            .apply {
                                if (prop is DtoProp<*, *>) {
                                    if (isSimpleProp(prop as DtoProp<ImmutableType, ImmutableProp>)) {
                                        add("base.%L", prop.name)
                                    } else {
                                        add(
                                            "%L.get(base)",
                                            StringUtil.snake("${prop.name}Accessor", SnakeCase.UPPER)
                                        )
                                    }
                                } else {
                                    add("%N", prop.alias)
                                }
                            }
                            .unindent()
                            .build()
                    })
                }
                .build()
        )
    }

    private fun addToEntity() {
        typeBuilder.addFunction(
            FunSpec
                .builder("toEntity")
                .returns(dtoType.baseType.className)
                .addModifiers(KModifier.OVERRIDE)
                .beginControlFlow(
                    "return %M(%T::class).by",
                    NEW,
                    dtoType.baseType.className
                )
                .apply {
                    addStatement(
                        "val that = this@%N",
                        if (innerClassName !== null && innerClassName.isNotEmpty()) {
                            innerClassName
                        } else {
                            dtoType.name!!
                        }
                    )
                    for (prop in dtoType.dtoProps) {
                        val baseProp = prop.toTailProp().baseProp
                        if (isSimpleProp(prop)) {
                            if (prop.isNullable && baseProp.let { it.isList && it.isAssociation(true) }) {
                                addStatement("%L = that.%L ?: emptyList()", baseProp.name, prop.name)
                            } else {
                                addStatement("%L = that.%L", baseProp.name, prop.name)
                            }
                        } else {
                            if (prop.isNullable && baseProp.let { it.isList && it.isAssociation(true) }) {
                                addStatement(
                                    "%L.set(this, that.%L ?: emptyList<%T>())",
                                    StringUtil.snake("${prop.name}Accessor", SnakeCase.UPPER),
                                    prop.name,
                                    if (prop.isIdOnly) {
                                        baseProp.targetType!!.idProp!!.typeName()
                                    } else {
                                        baseProp.targetType!!.className
                                    }
                                )
                            } else {
                                addStatement(
                                    "%L.set(this, that.%L)",
                                    StringUtil.snake("${prop.name}Accessor", SnakeCase.UPPER),
                                    prop.name
                                )
                            }
                        }
                    }
                }
                .endControlFlow()
                .build()
        )
    }

    private fun addApplyTo() {
        typeBuilder.addFunction(
            FunSpec
                .builder("applyTo")
                .addParameter("args", K_SPECIFICATION_ARGS_CLASS_NAME.parameterizedBy(dtoType.baseType.className))
                .addModifiers(KModifier.OVERRIDE)
                .apply {
                    addStatement("val __applier = args.applier")
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
                        addPredicateOperation(prop.toTailProp())
                    }
                    addStackOperations(stack, emptyList())
                }
                .build()
        )
    }

    private fun FunSpec.Builder.addStackOperations(
        stack: List<ImmutableProp>,
        newStack: List<ImmutableProp>
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
            addStatement("__applier.pop()")
        }
        for (prop in newStack.subList(sameCount, newStack.size)) {
            addStatement(
                "__applier.push(%T.%L)",
                prop.declaringType.propsClassName,
                StringUtil.snake(prop.name, SnakeCase.UPPER)
            )
        }
        return newStack
    }

    private fun FunSpec.Builder.addPredicateOperation(prop: DtoProp<ImmutableType, ImmutableProp>) {

        val targetType = prop.targetType
        if (targetType !== null) {
            addStatement("this.%L?.let { it.applyTo(args.child()) }", prop.name)
            return
        }

        val funcName = when (prop.funcName) {
            null -> "eq"
            "null" -> "isNull"
            "notNull" -> "isNotNull"
            "id" -> "associatedIdEq"
            else -> prop.funcName
        }

        addCode(
            CodeBlock.builder()
                .apply {
                    add("__applier.%L(", funcName)
                    if (Constants.MULTI_ARGS_FUNC_NAMES.contains(funcName)) {
                        add("arrayOf(")
                        prop.basePropMap.values.forEachIndexed { index, baseProp ->
                            if (index != 0) {
                                add(", ")
                            }
                            add(
                                "%T.%L",
                                baseProp.declaringType.propsClassName,
                                StringUtil.snake(baseProp.name, SnakeCase.UPPER)
                            )
                        }
                        add(")")
                    } else {
                        add(
                            "%T.%L",
                            prop.baseProp.declaringType.propsClassName,
                            StringUtil.snake(prop.baseProp.name, SnakeCase.UPPER)
                        )
                    }
                    add(", this.%L", prop.name)
                    if (funcName == "like") {
                        add(", ")
                        add(if (prop.likeOptions.contains(LikeOption.INSENSITIVE)) "true" else "false")
                        add(", ")
                        add(if (prop.likeOptions.contains(LikeOption.MATCH_START)) "true" else "false")
                        add(", ")
                        add(if (prop.likeOptions.contains(LikeOption.MATCH_END)) "true" else "false")
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
        return if (prop.isNullable() && (!prop.getBaseProp().isNullable ||
                dtoType.modifiers.contains(DtoTypeModifier.SPECIFICATION))) {
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
            StringUtil.snake("${prop.name}Accessor", StringUtil.SnakeCase.UPPER),
            DTO_PROP_ACCESSOR,
            KModifier.PRIVATE
        ).initializer(
            CodeBlock
                .builder()
                .apply {
                    add("%T(", DTO_PROP_ACCESSOR)
                    indent()

                    if (prop.isNullable() && (!prop.toTailProp().getBaseProp().isNullable ||
                            dtoType.modifiers.contains(DtoTypeModifier.SPECIFICATION) ||
                            dtoType.modifiers.contains(DtoTypeModifier.DYNAMIC))
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
                        if (dtoType.modifiers.contains(DtoTypeModifier.SPECIFICATION)) {
                            add(",\nnull")
                        } else {
                            add(
                                ",\n%T.%L(%T::class.java)",
                                DTO_PROP_ACCESSOR,
                                if (tailBaseProp.isList) "idListGetter" else "idReferenceGetter",
                                tailBaseProp.targetTypeName(overrideNullable = false)
                            )
                        }
                        add(
                            ",\n%T.%L(%T::class.java)",
                            DTO_PROP_ACCESSOR,
                            if (tailBaseProp.isList) "idListSetter" else "idReferenceSetter",
                            tailBaseProp.targetTypeName(overrideNullable = false)
                        )
                    } else if (tailBaseProp.targetType !== null) {
                        if (dtoType.modifiers.contains(DtoTypeModifier.SPECIFICATION)) {
                            add(",\nnull")
                        } else {
                            add(
                                ",\n%T.%L<%T, %L> {",
                                DTO_PROP_ACCESSOR,
                                if (tailBaseProp.isList) "objectListGetter" else "objectReferenceGetter",
                                tailBaseProp.targetTypeName(overrideNullable = false),
                                targetSimpleName(prop)
                            )
                            indent()
                            add("\n%L(it)", targetSimpleName(prop))
                            unindent()
                            add("\n}")
                        }
                        add(
                            ",\n%T.%L<%T, %L> {",
                            DTO_PROP_ACCESSOR,
                            if (tailBaseProp.isList) "objectListSetter" else "objectReferenceSetter",
                            tailBaseProp.targetTypeName(overrideNullable = false),
                            targetSimpleName(prop)
                        )
                        indent()
                        add("\nit.toEntity()")
                        unindent()
                        add("\n}")
                    } else if (prop.enumType !== null) {
                        val enumType = prop.enumType!!
                        val enumTypeName = tailBaseProp.targetTypeName(overrideNullable = false)
                        if (dtoType.modifiers.contains(DtoTypeModifier.SPECIFICATION)) {
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
                        beginControlFlow(
                            "when (it as %T)",
                            if (propTypeName(prop).copy(nullable = false) == INT) INT else STRING
                        )
                        for ((v, en) in enumType.constantMap) {
                            addStatement("%L -> %T.%L", v, enumTypeName, en)
                        }
                        addStatement("else -> IllegalArgumentException(")
                        indent()
                        addStatement("%S", "Illegal value \${it} for the enum type $enumTypeName")
                        unindent()
                        add(")\n")
                        endControlFlow()
                        unindent()
                        add("}")
                    }

                    unindent()
                    add("\n)")
                }
                .build()
        )
        addProperty(builder.build())
    }

    private fun propTypeName(prop: DtoProp<ImmutableType, ImmutableProp>): TypeName {

        val baseProp = prop.toTailProp().baseProp
        if (dtoType.modifiers.contains(DtoTypeModifier.SPECIFICATION)) {
            val funcName = prop.toTailProp().getFuncName()
            if (funcName != null) {
                when (funcName) {
                    "null", "notNull" ->
                        return BOOLEAN

                    "valueIn", "valueNotIn" ->
                        return COLLECTION.parameterizedBy(propElementName(prop)).copy(nullable = prop.isNullable)

                    "id", "associatedIdEq", "associatedIdNe" ->
                        return baseProp.targetType!!.idProp!!.typeName().copy(nullable = prop.isNullable)

                    "associatedIdIn", "associatedIdNotIn" ->
                        return COLLECTION.parameterizedBy(baseProp.targetType!!.idProp!!.typeName())
                            .copy(nullable = prop.isNullable)
                }
            }
            if (baseProp.isAssociation(true)) {
                return propElementName(prop).copy(nullable = prop.isNullable)
            }
        }

        val enumType = prop.enumType
        if (enumType !== null) {
            return (if (enumType.isNumeric) INT else STRING).copy(nullable = prop.isNullable)
        }
        val elementTypeName: TypeName = propElementName(prop)
        return if (baseProp.isList) {
            LIST.parameterizedBy(elementTypeName)
        } else {
            elementTypeName
        }.let {
            if (prop.isNullable) {
                it.copy(nullable = true)
            } else {
                it
            }
        }
    }

    private fun propElementName(prop: DtoProp<ImmutableType, ImmutableProp>): TypeName {
        val tailProp = prop.toTailProp()
        val targetType = tailProp.targetType
        if (targetType !== null) {
            if (targetType.name === null) {
                val list: MutableList<String> = ArrayList()
                collectNames(list)
                if (tailProp.isNewTarget) {
                    list.add(targetSimpleName(tailProp))
                }
                return ClassName(
                    packageName(),
                    list[0],
                    *list.subList(1, list.size).toTypedArray()
                )
            }
            return ClassName(
                packageName(),
                targetType.name!!
            )
        }
        return if (tailProp.isIdOnly) {
                tailProp.baseProp.targetType!!.idProp!!.targetTypeName(overrideNullable = false)
            } else {
                tailProp.baseProp.targetTypeName(overrideNullable = false)
            }
    }

    private fun typeName(typeRef: TypeRef?): TypeName {
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

    private fun collectNames(list: MutableList<String>) {
        if (parent == null) {
            list.add(dtoType.name!!)
        } else if (innerClassName !== null){
            parent.collectNames(list)
            list.add(innerClassName)
        }
    }

    companion object {
        @JvmStatic
        private fun targetSimpleName(prop: DtoProp<ImmutableType, ImmutableProp>): String {
            prop.targetType ?: throw IllegalArgumentException("prop is not association")
            return "TargetOf_${prop.name}"
        }

        @JvmStatic
        private val NEW = MemberName("org.babyfish.jimmer.kt", "new")

        @JvmStatic
        private val NEW_FETCHER = MemberName("org.babyfish.jimmer.sql.kt.fetcher", "newFetcher")

        private fun isCopyableAnnotation(annotation: KSAnnotation): Boolean {
            val declaration = annotation.annotationType.resolve().declaration
            val target = declaration.annotation(kotlin.annotation.Target::class)
            if (target !== null) {
                val accept = target
                    .get<List<KSType>>("allowedTargets")
                    ?.any { it.toString().endsWith("FIELD") } == true
                if (accept) {
                    val qualifiedName = declaration.qualifiedName!!.asString()
                    return !qualifiedName.startsWith("org.babyfish.jimmer.") ||
                        qualifiedName.startsWith("org.babyfish.jimmer.client.")
                }
            }
            return false
        }

        private fun annotationOf(anno: Anno, target: AnnotationSpec.UseSiteTarget? = null): AnnotationSpec =
            AnnotationSpec
                .builder(ClassName.bestGuess(anno.qualifiedName))
                .apply {
                    if (anno.valueMap.isNotEmpty()) {
                        addMember(
                            CodeBlock
                                .builder()
                                .apply {
                                    add("\n")
                                    add(anno.valueMap)
                                    add("\n")
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
                    } else {
                        add("(\n")
                        add(value.anno.valueMap)
                        add("\n)")
                    }
                }
                is EnumValue -> add(
                    "%T.%N",
                    ClassName.bestGuess(value.qualifiedName),
                    value.constant
                )
                else -> add((value as LiteralValue).value)
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

        private fun defaultValue(prop: UserProp): String? {
            val typeRef = prop.typeRef
            return if (typeRef.isNullable) {
                "null"
            } else {
                when (typeRef.typeName) {
                    TypeRef.TN_BOOLEAN -> "false"
                    TypeRef.TN_CHAR -> "'\\0'"

                    TypeRef.TN_BYTE, TypeRef.TN_SHORT, TypeRef.TN_INT, TypeRef.TN_LONG,
                    TypeRef.TN_FLOAT, TypeRef.TN_DOUBLE -> "0"

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
    }
}