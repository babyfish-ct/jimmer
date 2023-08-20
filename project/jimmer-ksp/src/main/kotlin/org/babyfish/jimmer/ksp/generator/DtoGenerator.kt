package org.babyfish.jimmer.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import org.babyfish.jimmer.dto.compiler.DtoProp
import org.babyfish.jimmer.dto.compiler.DtoType
import org.babyfish.jimmer.dto.compiler.TypeRef
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
            (if (dtoType.isInput) INPUT_CLASS_NAME else VIEW_CLASS_NAME).parameterizedBy(
                dtoType.baseType.className
            )
        )

        addMetadata()

        addPrimaryConstructor()
        addConverterConstructor()

        addUserProps()

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
        add("%N(", prop.getBaseProp().name)
        indent()
        add("%N(%T::class).by {\n", NEW_FETCHER, prop.getBaseProp().targetType!!.className)
        indent()
        for (childProp in targetDtoType.dtoProps) {
            addHiddenFetcherField(childProp)
        }
        unindent()
        add("}\n")
        unindent()
        add(")")
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
        typeBuilder.primaryConstructor(builder.build())

        for (prop in dtoType.dtoProps) {
            typeBuilder.addProperty(
                PropertySpec
                    .builder(prop.name, propTypeName(prop))
                    .mutable(mutable)
                    .initializer(prop.name)
                    .apply {
                        for (anno in prop.baseProp.annotations { isCopyableAnnotation(it) }) {
                            addAnnotation(anno.toAnnotationSpec())
                        }
                    }
                    .build()
            )
        }
    }

    private fun addConverterConstructor() {
        typeBuilder.addFunction(
            FunSpec
                .constructorBuilder()
                .addParameter("base", dtoType.baseType.className)
                .apply {
                    callThisConstructor(dtoType.dtoProps.map { prop ->
                        CodeBlock
                            .builder()
                            .indent()
                            .add("\n")
                            .apply {
                                val targetType = prop.targetType
                                when {
                                    prop.nextProp !== null -> {
                                        add("%T.get(\n", FLAT_UTILS_CLASS_NAME)
                                        indent()
                                        add("base,\n")
                                        add("intArrayOf(\n")
                                        indent()
                                        var p = prop
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
                                            add(" ?: error(%S)", "The property chain \"${prop.basePath}\" is null or unloaded")
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
                            }
                            .unindent()
                            .build()
                    })
                }
                .build()
        )
    }

    private fun addUserProps() {
        for (userProp in dtoType.userProps) {
            typeBuilder.addProperty(
                PropertySpec
                    .builder(
                        userProp.alias,
                        typeName(userProp.typeRef)
                    )
                    .mutable()
                    .apply {
                        val typeRef = userProp.typeRef
                        if (typeRef.isNullable) {
                            initializer("null")
                        } else {
                            when (typeRef.typeName) {
                                TypeRef.TN_BOOLEAN -> initializer("false")
                                TypeRef.TN_CHAR -> initializer("'\\0'")

                                TypeRef.TN_BYTE, TypeRef.TN_SHORT, TypeRef.TN_INT, TypeRef.TN_LONG,
                                TypeRef.TN_FLOAT, TypeRef.TN_DOUBLE -> initializer("0")

                                TypeRef.TN_STRING -> initializer("\"\"")

                                TypeRef.TN_ARRAY -> if (typeRef.arguments[0].typeRef.isNullable) {
                                    initializer("emptyArray()")
                                } else {
                                    when (typeRef.arguments[0].typeRef.typeName) {
                                        TypeRef.TN_BOOLEAN -> initializer("booleanArrayOf()")
                                        TypeRef.TN_CHAR -> initializer("charArrayOf()")
                                        TypeRef.TN_BYTE -> initializer("byteArrayOf()")
                                        TypeRef.TN_SHORT -> initializer("shortArrayOf()")
                                        TypeRef.TN_INT -> initializer("intArrayOf()")
                                        TypeRef.TN_LONG -> initializer("longArrayOf()")
                                        TypeRef.TN_FLOAT -> initializer("floatArrayOf()")
                                        TypeRef.TN_DOUBLE -> initializer("doubleArrayOf()")
                                        else -> initializer("emptyArray()")
                                    }
                                }

                                TypeRef.TN_ITERABLE, TypeRef.TN_COLLECTION, TypeRef.TN_LIST ->
                                    initializer("emptyList()")
                                TypeRef.TN_MUTABLE_ITERABLE, TypeRef.TN_MUTABLE_COLLECTION, TypeRef.TN_MUTABLE_LIST ->
                                    initializer("mutableListOf()")

                                TypeRef.TN_SET -> initializer("emptySet()")
                                TypeRef.TN_MUTABLE_SET -> initializer("mutableSetOf()")

                                TypeRef.TN_MAP -> initializer("emptyMap()")
                                TypeRef.TN_MUTABLE_MAP -> initializer("mutableMapOf()")

                                else -> addModifiers(KModifier.LATEINIT)
                            }
                        }
                    }
                    .build()
            )
        }
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
                        "val that = this@%L",
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
                            addStatement("val that_%N = that.%N", prop.name, prop.name)
                            beginControlFlow("if (that_%N !== null)", prop.name)
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
        val that = if (prop.isNullable && !prop.baseProp.isNullable) "that_" else "that."
        when {
            targetType !== null ->
                if (prop.baseProp.isList) {
                    addStatement(
                        "this.%N = %L%N%Lmap { it.toEntity() }",
                        prop.baseProp.name,
                        that,
                        prop.name,
                        if (prop.baseProp.isNullable) "?." else "."
                    )
                } else {
                    addStatement(
                        "this.%N = %L%N%LtoEntity()",
                        prop.baseProp.name,
                        that,
                        prop.name,
                        if (prop.baseProp.isNullable) "?." else "."
                    )
                }
            prop.isIdOnly -> {
                beginControlFlow(
                    "this.%N = %L%N%L%N",
                    prop.baseProp.name,
                    that,
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
                addStatement("this.%N = %L%N", prop.baseProp.name, that, prop.name)
        }
    }

    private fun propTypeName(prop: DtoProp<ImmutableType, ImmutableProp>): TypeName {
        val elementTypeName: TypeName = propElementName(prop.toTailProp())
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
        val targetType = prop.targetType
        if (targetType !== null) {
            if (targetType.name === null) {
                val list: MutableList<String> = ArrayList()
                collectNames(list)
                if (prop.isNewTarget) {
                    list.add(targetSimpleName(prop))
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
        return if (prop.isIdOnly) {
                prop.baseProp.targetType!!.idProp!!.targetTypeName(overrideNullable = false)
            } else {
                prop.baseProp.targetTypeName(overrideNullable = false)
            }
    }

    private fun typeName(typeRef: TypeRef): TypeName {
        val typeName = when (typeRef.typeName) {
            TypeRef.TN_BOOLEAN -> BOOLEAN
            TypeRef.TN_CHAR -> CHAR
            TypeRef.TN_BYTE -> BYTE
            TypeRef.TN_SHORT -> SHORT
            TypeRef.TN_INT -> INT
            TypeRef.TN_LONG -> LONG
            TypeRef.TN_FLOAT -> FLOAT
            TypeRef.TN_DOUBLE -> DOUBLE
            TypeRef.TN_STRING -> STRING
            TypeRef.TN_ARRAY ->
                if (typeRef.arguments[0].typeRef.isNullable) {
                    ARRAY.parameterizedBy(typeName(typeRef.arguments[0].typeRef))
                } else {
                    when (typeRef.arguments[0].typeRef.typeName) {
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
        val args = typeRef
            .arguments
            .takeIf { it.isNotEmpty() && typeRef.typeName != TypeRef.TN_ARRAY }
            ?.let { args ->
                Array(args.size) { i ->
                    typeName(args[i].typeRef).let {
                        when {
                            args[i].isIn -> WildcardTypeName.consumerOf(it)
                            args[i].isOut -> WildcardTypeName.producerOf(it)
                            else ->it
                        }
                    }
                }
            }
        return if (args == null) {
            typeName
        } else {
            (typeName as ClassName).parameterizedBy(*args)
        }.copy(
            nullable = typeRef.isNullable
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
                        qualifiedName != Nullable::class.java.name &&
                        !qualifiedName.startsWith("org.babyfish.jimmer.")
                }
            }
            return false
        }
    }
}