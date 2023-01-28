package org.babyfish.jimmer.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import org.babyfish.jimmer.ksp.annotation
import org.babyfish.jimmer.ksp.get
import org.babyfish.jimmer.ksp.meta.*
import org.babyfish.jimmer.pojo.AutoScalarStrategy
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.io.OutputStreamWriter
import java.util.*

class StaticDeclarationGenerator private constructor(
    private val declaration: StaticDeclaration,
    private val codeGenerator: CodeGenerator?,
    private val file: KSFile?,
    private val innerClassName: String?,
    private val parent: StaticDeclarationGenerator?
) {
    private var _typeBuilder: TypeSpec.Builder? = null
    
    private val immutableType: ImmutableType
        get() = declaration.immutableType

    private val props: List<StaticProp> =
        mutableListOf<StaticProp>().apply {
            val alias = declaration.alias
            val hasKey = declaration
                .immutableType
                .properties
                .values
                .any {
                    it.isKey
                }

            val possibleAutoScalars = mutableSetOf<ImmutableProp>().apply {
                var type: ImmutableType? = declaration.immutableType
                while (type !== null) {
                    val strategy = type.autoScalarStrategy(declaration.alias)
                    if (strategy == AutoScalarStrategy.NONE) {
                        break
                    }
                    for (prop in type.properties.values) {
                        if (!prop.isAssociation(true) && !prop.isTransient) {
                            add(prop)
                        }
                    }
                    if (strategy == AutoScalarStrategy.DECLARED) {
                        break
                    }
                    type = type.superType
                }
            }
            for (prop in immutableType.properties.values) {
                var staticProp = prop.staticProp(alias)
                if (staticProp == null) {
                    if (possibleAutoScalars.contains(prop)) {
                        staticProp = StaticProp(
                            prop,
                            alias,
                            prop.name,
                            true,
                            declaration.allOptional,
                            false,
                            ""
                        )
                        if (!staticProp.isOptional && prop.isId && hasKey) {
                            staticProp = staticProp.copy(isOptional = true)
                        }
                        this += (staticProp)
                    }
                } else if (staticProp.isEnabled) {
                    if (prop.isTransient) {
                        if (isInput) {
                            throw MetaException(
                                "Illegal property \"" +
                                    prop +
                                    "\", the transient property of input type can not be decorated by @Static"
                            )
                        }
                        if (!prop.hasTransientResolver) {
                            throw MetaException(
                                "Illegal property \"" +
                                    prop +
                                    "\", if a property is decorated by both @Transient and @Static," +
                                    "its transient resolver must be specified"
                            )
                        }
                    }
                    this += staticProp.copy(isOptional = declaration.allOptional)
                }
            }
        }

    constructor(
        declaration: StaticDeclaration,
        codeGenerator: CodeGenerator?,
        file: KSFile?
    ): this(declaration, codeGenerator, file, null, null)

    constructor(
        declaration: StaticDeclaration,
        innerClasName: String,
        parent: StaticDeclarationGenerator
    ): this(declaration, null, null, innerClasName, parent)

    internal val typeBuilder: TypeSpec.Builder
        get() = _typeBuilder ?: error("Type builder is not ready")

    internal val isInput: Boolean
        get() = parent?.isInput ?: declaration.topLevelName.endsWith("Input")

    internal fun getClassName(vararg nestedNames: String): ClassName {
            if (innerClassName !== null) {
                val list: MutableList<String> = ArrayList()
                collectNames(list)
                list.addAll(nestedNames.toList())
                return ClassName(
                    declaration.immutableType.className.packageName,
                    list[0],
                    *list.subList(1, list.size).toTypedArray()
                )
            }
            return ClassName(
                declaration.immutableType.className.packageName,
                declaration.topLevelName,
                *nestedNames
            )
        }

    fun generate(allFiles: List<KSFile>) {
        if (codeGenerator != null && file !== null) {
            codeGenerator.createNewFile(
                Dependencies(false, *allFiles.toTypedArray()),
                file.packageName.asString(),
                declaration.topLevelName
            ).use {
                val fileSpec = FileSpec
                    .builder(
                        file.packageName.asString(),
                        declaration.topLevelName
                    ).apply {
                        indent("    ")
                        val builder = TypeSpec
                            .classBuilder(declaration.topLevelName)
                            .addModifiers(KModifier.DATA)
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
                .addModifiers(KModifier.DATA)
            _typeBuilder = builder
            try {
                addMembers(allFiles)
                parent.typeBuilder.addType(builder.build())
            } finally {
                _typeBuilder = null
            }
        }
    }

    private fun addMembers(allFiles: List<KSFile>) {

        typeBuilder.addSuperinterface(
            (if (isInput) INPUT_CLASS_NAME else STATIC_CLASS_NAME).parameterizedBy(
                immutableType.className
            )
        )

        addMetadata()

        addJsonConstructor()
        addConverterConstructor()

        addToEntity()
        addToEntityWithBase()

        for (prop in props) {
            val target = prop.target
            if (target !== null && target.topLevelName.isEmpty()) {
                StaticDeclarationGenerator(
                    target,
                    targetSimpleName(prop),
                    this
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
                                STATIC_METADATA_CLASS_NAME.parameterizedBy(
                                    declaration.immutableType.className,
                                    getClassName()
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
                                            STATIC_METADATA_CLASS_NAME,
                                            declaration.immutableType.className, getClassName()
                                        )
                                        indent()
                                        add("%M(%T::class).by", NEW_FETCHER, declaration.immutableType.className)
                                        beginControlFlow("")
                                        for (prop in props) {
                                            if (!prop.immutableProp.isId) {
                                                if (prop.target !== null) {
                                                    addStatement(
                                                        "%N(%T.METADATA.fetcher)",
                                                        prop.immutableProp.name,
                                                        propElementName(prop)
                                                    )
                                                } else {
                                                    addStatement("%N()", prop.immutableProp.name)
                                                }
                                            }
                                        }
                                        endControlFlow()
                                        unindent()
                                        add(")")
                                        beginControlFlow("")
                                        addStatement("%T(it)", getClassName())
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

    private fun addJsonConstructor() {
        val builder = FunSpec.constructorBuilder()
        for (prop in props) {
            builder.addParameter(prop.name, propTypeName(prop))
        }
        typeBuilder.primaryConstructor(builder.build())

        for (prop in props) {
            typeBuilder.addProperty(
                PropertySpec
                    .builder(prop.name, propTypeName(prop))
                    .initializer(prop.name)
                    .apply {
                        for (anno in prop.immutableProp.annotations { isCopyableAnnotation(it) }) {
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
                .addParameter("base", immutableType.className)
                .apply {
                    callThisConstructor(props.map { prop ->
                        CodeBlock
                            .builder()
                            .indent()
                            .add("\n")
                            .apply {
                                val target = prop.target
                                when {
                                    target !== null ->
                                        if (prop.isNullable(isInput)) {
                                            add(
                                                "base.takeIf { (it as %T).__isLoaded(%L) }?.%N?.%N { %T(it) }",
                                                IMMUTABLE_SPI_CLASS_NAME,
                                                prop.immutableProp.id,
                                                prop.immutableProp.name,
                                                if (prop.immutableProp.isList) "map" else "let",
                                                propElementName(prop)
                                            )
                                        } else {
                                            add(
                                                "base.%N%L%N { %T(it) }",
                                                prop.immutableProp.name,
                                                if (prop.isNullable(false)) "?." else ".",
                                                if (prop.immutableProp.isList) "map" else "let",
                                                propElementName(prop)
                                            )
                                        }
                                    prop.isIdOnly ->
                                        if (prop.isNullable(isInput)) {
                                            add(
                                                "base.takeIf { (it as %T).__isLoaded(%L) }?.%N?.%L",
                                                IMMUTABLE_SPI_CLASS_NAME,
                                                prop.immutableProp.id,
                                                prop.immutableProp.name,
                                                if (prop.immutableProp.isList) {
                                                    "map{ it.${prop.immutableProp.targetType!!.idProp!!.name} }"
                                                } else {
                                                    prop.immutableProp.targetType!!.idProp!!.name
                                                }
                                            )
                                        } else {
                                            add(
                                                "base.%N%L%L",
                                                prop.immutableProp.name,
                                                if (prop.isNullable(false)) "?." else ".",
                                                if (prop.immutableProp.isList) {
                                                    "map{ it.${prop.immutableProp.targetType!!.idProp!!.name} }"
                                                } else {
                                                    prop.immutableProp.targetType!!.idProp!!.name
                                                }
                                            )
                                        }
                                    else ->
                                        if (prop.isNullable(isInput)) {
                                            add(
                                                "base.takeIf { (it as %T).__isLoaded(%L) }?.%N",
                                                IMMUTABLE_SPI_CLASS_NAME,
                                                prop.immutableProp.id,
                                                prop.immutableProp.name
                                            )
                                        } else {
                                            add(
                                                "base%L%N",
                                                if (prop.isNullable(false)) "?." else ".",
                                                prop.immutableProp.name
                                            )
                                        }
                                }
                                if (!prop.isNullable(true) && prop.isNullable(false)) {
                                    add(" ?: error(%S)", "\"base.${prop.immutableProp.name}\" cannot be null")
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
                .returns(immutableType.className)
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("return toEntity(null)")
                .build()
        )
    }

    private fun addToEntityWithBase() {
        typeBuilder.addFunction(
            FunSpec
                .builder("toEntity")
                .returns(immutableType.className)
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(
                    ParameterSpec
                        .builder(
                            "base",
                            immutableType.className.copy(nullable = true)
                        )
                        .build()
                )
                .beginControlFlow(
                    "return %M(%T::class).%M(base) ",
                    NEW,
                    immutableType.className,
                    newBy()
                )
                .apply {
                    addStatement(
                        "val that = this@%L",
                        if (innerClassName !== null && innerClassName.isNotEmpty()) {
                            innerClassName
                        } else {
                            declaration.topLevelName
                        }
                    )
                    for (prop in props) {
                        if (prop.isNullable(isInput) && !prop.immutableProp.isNullable) {
                            beginControlFlow("if (that.%N !== null)", prop.name)
                            addAssignment(prop)
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

    private fun FunSpec.Builder.addAssignment(prop: StaticProp) {
        val target = prop.target
        when {
            target !== null ->
                if (prop.immutableProp.isList) {
                    addStatement(
                        "this.%N = that.%N%Lmap { it.toEntity() }",
                        prop.immutableProp.name,
                        prop.name,
                        if (prop.isNullable(isInput)) "?." else "."
                    )
                } else {
                    addStatement(
                        "this.%N = that.%N%LtoEntity()",
                        prop.immutableProp.name,
                        prop.name,
                        if (prop.isNullable(isInput)) "?." else "."
                    )
                }
            prop.isIdOnly -> {
                beginControlFlow(
                    "this.%N = that.%N%L%N",
                    prop.immutableProp.name,
                    prop.name,
                    if (prop.isNullable(isInput)) "?." else ".",
                    if (prop.immutableProp.isNullable) "let" else "map"
                )
                beginControlFlow(
                    "%M(%T::class).%M",
                    NEW,
                    prop.immutableProp.targetType!!.className,
                    newBy(prop.immutableProp.targetType)
                )
                addStatement("this.%N = it", prop.immutableProp.targetType!!.idProp!!.name)
                endControlFlow()
                endControlFlow()
            }
            else ->
                addStatement("this.%N = that.%N", prop.immutableProp.name, prop.name)
        }
    }

    private fun propTypeName(prop: StaticProp): TypeName {
        val elementTypeName: TypeName = propElementName(prop)
        return if (prop.immutableProp.isList) {
            LIST_CLASS_NAME.parameterizedBy(elementTypeName)
        } else {
            elementTypeName
        }.let {
            if (prop.isNullable(isInput)) {
                it.copy(nullable = true)
            } else {
                it
            }
        }
    }

    private fun propElementName(prop: StaticProp): TypeName {
        val target = prop.target
        if (target !== null) {
            if (target.topLevelName.isEmpty()) {
                val list: MutableList<String> = ArrayList()
                collectNames(list)
                list.add(targetSimpleName(prop))
                return ClassName(
                    immutableType.className.packageName,
                    list[0],
                    *list.subList(1, list.size).toTypedArray()
                )
            }
            return ClassName(
                target.immutableType.className.packageName,
                target.topLevelName
            )
        }
        return if (prop.isIdOnly) {
                prop.immutableProp.targetType!!.idProp!!.targetTypeName(overrideNullable = false)
            } else {
                prop.immutableProp.targetTypeName(overrideNullable = false)
            }
    }

    private fun collectNames(list: MutableList<String>) {
        if (parent == null) {
            list.add(declaration.topLevelName)
        } else if (innerClassName !== null){
            parent.collectNames(list)
            list.add(innerClassName)
        }
    }

    private fun newBy(type: ImmutableType? = null): MemberName =
        MemberName((type ?: immutableType).className.packageName, "by")

    companion object {
        @JvmStatic
        private fun targetSimpleName(prop: StaticProp): String {
            val target = prop.target ?: throw IllegalArgumentException("prop is not association")
            return target.topLevelName.ifEmpty { "TargetOf_" + prop.name }
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