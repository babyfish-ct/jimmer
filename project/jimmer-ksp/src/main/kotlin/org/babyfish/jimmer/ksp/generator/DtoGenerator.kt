package org.babyfish.jimmer.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import org.babyfish.jimmer.Input
import org.babyfish.jimmer.View
import org.babyfish.jimmer.ViewableInput
import org.babyfish.jimmer.dto.compiler.*
import org.babyfish.jimmer.dto.compiler.Anno.AnnoValue
import org.babyfish.jimmer.dto.compiler.Anno.ArrayValue
import org.babyfish.jimmer.dto.compiler.Anno.EnumValue
import org.babyfish.jimmer.dto.compiler.Anno.LiteralValue
import org.babyfish.jimmer.dto.compiler.Anno.Value
import org.babyfish.jimmer.ksp.annotation
import org.babyfish.jimmer.ksp.get
import org.babyfish.jimmer.ksp.meta.*
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.io.OutputStreamWriter
import java.util.*

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

        typeBuilder.addSuperinterface(
            when {
                dtoType.modifiers.contains(DtoTypeModifier.INPUT_ONLY) ->
                    Input::class
                dtoType.modifiers.contains(DtoTypeModifier.INPUT) ->
                    ViewableInput::class
                else ->
                    View::class
            }.asClassName().parameterizedBy(
                dtoType.baseType.className
            )
        )

        val isInputOnly = dtoType.modifiers.contains(DtoTypeModifier.INPUT_ONLY)
        if (!isInputOnly) {
            addMetadata()
        }
        addPrimaryConstructor()
        if (!isInputOnly) {
            addConverterConstructor()
        }

        addToEntity()

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

    private fun addMetadata() {
        typeBuilder.addType(
            TypeSpec
                .companionObjectBuilder()
                .apply {
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
        for (prop in dtoType.dtoProps) {
            builder.addParameter(
                ParameterSpec
                    .builder(prop.name, propTypeName(prop))
                    .apply {
                        when {
                            prop.isNullable -> defaultValue("null")
                            prop.toTailProp().baseProp.isList -> defaultValue("emptyList()")
                            prop.toTailProp().baseProp.isPrimitive -> defaultValue(
                                when (prop.baseProp.typeName()) {
                                    BOOLEAN -> "false"
                                    CHAR -> "'\\0'"
                                    else -> "0"
                                }
                            )
                            prop.toTailProp().baseProp.typeName() == STRING ->
                                defaultValue("\"\"")
                        }
                    }
                    .build()
            )
        }
        for (userProp in dtoType.userProps) {
            builder.addParameter(
                ParameterSpec
                    .builder(userProp.alias, typeName(userProp.typeRef))
                    .apply {
                        defaultValue(userProp.typeRef)?.let {
                            defaultValue(it)
                        }
                    }
                    .build()
            )
        }
        typeBuilder.primaryConstructor(builder.build())

        for (prop in dtoType.dtoProps) {
            typeBuilder.addProperty(
                PropertySpec
                    .builder(prop.name, propTypeName(prop))
                    .mutable(mutable)
                    .initializer(prop.name)
                    .apply {
                        if (prop.annotations.isEmpty()) {
                            for (anno in prop.baseProp.annotations { isCopyableAnnotation(it) }) {
                                addAnnotation(anno.toAnnotationSpec())
                            }
                        } else {
                            for (anno in prop.annotations) {
                                addAnnotation(annotationOf(anno))
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
                        for (anno in prop.annotations) {
                            addAnnotation(annotationOf(anno))
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
                                    defaultValue(userProp.typeRef)?.let {
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
                                    prop as DtoProp<ImmutableType, ImmutableProp>
                                    val targetType = prop.targetType
                                    when {
                                        prop.nextProp !== null -> {
                                            add("%T.get(\n", FLAT_UTILS_CLASS_NAME)
                                            indent()
                                            add("base,\n")
                                            add("intArrayOf(\n")
                                            indent()
                                            var p: DtoProp<ImmutableType, ImmutableProp>? = prop
                                            while (p !== null) {
                                                add(
                                                    "%T.%N,\n",
                                                    p.baseProp.declaringType.draftClassName("$"),
                                                    p.baseProp.slotName
                                                )
                                                p = p.nextProp
                                            }
                                            unindent()
                                            add("),\n")
                                            if (prop.targetType == null) {
                                                add("null\n")
                                            }
                                            unindent()
                                            add(")")
                                            prop.targetType?.let {
                                                add(" {\n")
                                                indent()
                                                addStatement(
                                                    "%T(it as %T)\n",
                                                    propTypeName(prop),
                                                    prop.toTailProp().baseProp.targetType!!.className
                                                )
                                                unindent()
                                                add("}")
                                            }
                                            if (!prop.isNullable) {
                                                add(
                                                    " ?: error(%S)",
                                                    "The property chain \"${prop.basePath}\" is null or unloaded"
                                                )
                                            }
                                        }

                                        targetType !== null ->
                                            if (prop.isNullable) {
                                                beginControlFlow("base.takeIf")
                                                addStatement(
                                                    "(it as %T).__isLoaded(%T.byIndex(%T.%L))",
                                                    IMMUTABLE_SPI_CLASS_NAME,
                                                    PROP_ID_CLASS_NAME,
                                                    dtoType.baseType.draftClassName("$"),
                                                    prop.baseProp.slotName
                                                )
                                                endControlFlow()
                                                addStatement(
                                                    "?.%N?.%N { %T(it) }",
                                                    prop.baseProp.name,
                                                    if (prop.baseProp.isList) "map" else "let",
                                                    propElementName(prop)
                                                )
                                            } else {
                                                add(
                                                    "base.%N%L%N { %T(it) }",
                                                    prop.baseProp.name,
                                                    if (prop.baseProp.isNullable) "?." else ".",
                                                    if (prop.baseProp.isList) "map" else "let",
                                                    propElementName(prop)
                                                )
                                            }

                                        prop.isIdOnly ->
                                            if (prop.isNullable) {
                                                beginControlFlow("base.takeIf")
                                                addStatement(
                                                    "(it as %T).__isLoaded(%T.byIndex(%T.%L))",
                                                    IMMUTABLE_SPI_CLASS_NAME,
                                                    PROP_ID_CLASS_NAME,
                                                    dtoType.baseType.draftClassName("$"),
                                                    prop.baseProp.slotName
                                                )
                                                endControlFlow()
                                                addStatement(
                                                    "?.%N?.%L",
                                                    prop.baseProp.name,
                                                    if (prop.baseProp.isList) {
                                                        "map { it.${prop.baseProp.targetType!!.idProp!!.name} }"
                                                    } else {
                                                        prop.baseProp.targetType!!.idProp!!.name
                                                    }
                                                )
                                            } else {
                                                add(
                                                    "base.%N%L%L",
                                                    prop.baseProp.name,
                                                    if (prop.baseProp.isNullable) "?." else ".",
                                                    if (prop.baseProp.isList) {
                                                        "map { it.${prop.baseProp.targetType!!.idProp!!.name} }"
                                                    } else {
                                                        prop.baseProp.targetType!!.idProp!!.name
                                                    }
                                                )
                                            }

                                        else ->
                                            if (prop.isNullable) {
                                                beginControlFlow("base.takeIf")
                                                addStatement(
                                                    "(it as %T).__isLoaded(%T.byIndex(%T.%L))",
                                                    IMMUTABLE_SPI_CLASS_NAME,
                                                    PROP_ID_CLASS_NAME,
                                                    dtoType.baseType.draftClassName("$"),
                                                    prop.baseProp.slotName
                                                )
                                                endControlFlow()
                                                addStatement("?.%N", prop.baseProp.name)
                                            } else {
                                                add(
                                                    "base%L%N",
                                                    if (prop.baseProp.isNullable) "?." else ".",
                                                    prop.baseProp.name
                                                )
                                            }
                                    }
                                    if (!prop.isNullable && prop.baseProp.isNullable) {
                                        add(" ?: error(%S)", "\"base.${prop.basePath}\" cannot be null or unloaded")
                                    }
                                } else {
                                    addStatement("%N", prop.alias)
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
                        if (prop.nextProp !== null) {
                            addCode(
                                CodeBlock
                                    .builder()
                                    .apply {
                                        addFlatSetting(prop)
                                    }
                                    .build()
                            )
                        } else if (prop.isNullable && !prop.baseProp.isNullable) {
                            addStatement("val that_%L = that.%N", prop.name, prop.name)
                            beginControlFlow("if (that_%L !== null)", prop.name)
                            addAssignment(prop)
                            if (prop.baseProp.isList) {
                                nextControlFlow("else")
                                addStatement("this.%N = emptyList()", prop.baseProp.name)
                            }
                            endControlFlow()
                        } else {
                            addAssignment(prop)
                        }
                    }
                }
                .endControlFlow()
                .build()
        )
    }

    private fun CodeBlock.Builder.addFlatSetting(prop: DtoProp<ImmutableType, ImmutableProp>) {
        add("%T.set(\n", FLAT_UTILS_CLASS_NAME)
        indent()
        add("this,\n")
        add("intArrayOf(\n")
        indent()
        var p: DtoProp<ImmutableType, ImmutableProp>? = prop
        while (p != null) {
            add(
                "%T.%N,\n",
                p.baseProp.declaringType.draftClassName("$"),
                p.baseProp.slotName
            )
            p = p.nextProp
        }
        unindent()
        add("),\n")
        add("that.%N", prop.name)
        if (prop.targetType !== null) {
            if (prop.isNullable) {
                add("?")
            }
            add(".toEntity()")
        }
        add("\n")
        unindent()
        add(")\n")
    }

    private fun FunSpec.Builder.addAssignment(prop: DtoProp<ImmutableType, ImmutableProp>) {
        val targetType = prop.targetType
        val that = if (prop.isNullable && !prop.baseProp.isNullable) "that_%L" else "that.%N"
        when {
            targetType !== null ->
                if (prop.baseProp.isList) {
                    addStatement(
                        "this.%N = $that%Lmap { it.toEntity() }",
                        prop.baseProp.name,
                        prop.name,
                        if (prop.baseProp.isNullable) "?." else "."
                    )
                } else {
                    addStatement(
                        "this.%N = $that%LtoEntity()",
                        prop.baseProp.name,
                        prop.name,
                        if (prop.baseProp.isNullable) "?." else "."
                    )
                }
            prop.isIdOnly -> {
                beginControlFlow(
                    "this.%N = $that%L%N",
                    prop.baseProp.name,
                    prop.name,
                    if (prop.baseProp.isNullable) "?." else ".",
                    if (prop.baseProp.isList) "map" else "let"
                )
                beginControlFlow(
                    "%M(%T::class).by",
                    NEW,
                    prop.baseProp.targetType!!.className,
                )
                addStatement("this.%N = it", prop.baseProp.targetType!!.idProp!!.name)
                endControlFlow()
                endControlFlow()
            }
            else ->
                addStatement("this.%N = $that", prop.baseProp.name, prop.name)
        }
    }

    private fun propTypeName(prop: DtoProp<ImmutableType, ImmutableProp>): TypeName {
        val elementTypeName: TypeName = propElementName(prop)
        return if (prop.baseProp.isList) {
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
                    ?.any { it.toString().endsWith("FIELD") || it.toString().endsWith("METHOD") } == true
                if (accept) {
                    val qualifiedName = declaration.qualifiedName!!.asString()
                    return qualifiedName != NotNull::class.java.name &&
                        qualifiedName != Nullable::class.java.name && (
                            !qualifiedName.startsWith("org.babyfish.jimmer.") ||
                                qualifiedName.startsWith("org.babyfish.jimmer.client.")
                        )
                }
            }
            return false
        }

        private fun annotationOf(anno: Anno): AnnotationSpec =
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

        private fun defaultValue(typeRef: TypeRef): String? =
            if (typeRef.isNullable) {
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