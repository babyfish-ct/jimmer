package org.babyfish.jimmer.ksp.immutable.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType
import org.babyfish.jimmer.ksp.util.generatedAnnotation
import java.io.OutputStreamWriter

class DraftGenerator(
    private val codeGenerator: CodeGenerator,
    private val ctx: Context,
    private val file: KSFile,
    private val modelClassDeclarations: List<KSClassDeclaration>
) {
    fun generate(allFiles: List<KSFile>) {
        val draftFileName =
            file.fileName.let {
                var lastDotIndex = it.lastIndexOf('.')
                if (lastDotIndex == -1) {
                    "${it}$DRAFT"
                } else {
                    "${it.substring(0, lastDotIndex)}$DRAFT"
                }
            }
        codeGenerator.createNewFile(
            Dependencies(false, *allFiles.toTypedArray()),
            file.packageName.asString(),
            draftFileName
        ).use {
            val fileSpec = FileSpec
                .builder(
                    file.packageName.asString(),
                    draftFileName
                ).apply {
                    indent("    ")
                    addAnnotation(
                        AnnotationSpec
                            .builder(Suppress::class)
                            .apply {
                                addMember("\"RedundantVisibilityModifier\"")
                                addMember("\"Unused\"")
                            }
                            .build()
                    )
                    for (classDeclaration in modelClassDeclarations) {
                        val type = ctx.typeOf(classDeclaration)
                        addType(type)
                        if (!type.isMappedSuperclass) {
                            addNewByFun(type = type, companion = false, withBase = false, withBlock = true)
                            addNewByFun(type = type, companion = false, withBase = true, withBlock = false)
                            addNewByFun(type = type, companion = false, withBase = true, withBlock = true)

                            addNewByFun(type = type, companion = true, withBase = false, withBlock = true)
                            addNewByFun(type = type, companion = true, withBase = true, withBlock = true)

                            addAddFun(type = type, withBase = false, withBlock = true)
                            addAddFun(type = type, withBase = true, withBlock = false)
                            addAddFun(type = type, withBase = true, withBlock = true)

                            addCopyFun(type)
                        }
                    }
                }.build()
            val writer = OutputStreamWriter(it, Charsets.UTF_8)
            fileSpec.writeTo(writer)
            writer.flush()
        }
    }

    private fun FileSpec.Builder.addType(type: ImmutableType) {
        addType(
            TypeSpec
                .interfaceBuilder("${type.simpleName}$DRAFT")
                .addAnnotation(DSL_SCOPE_CLASS_NAME)
                .addSuperinterface(type.className)
                .addAnnotation(generatedAnnotation(type))
                .apply {
                    if (type.superTypes.isEmpty()) {
                        addSuperinterface(DRAFT_CLASS_NAME)
                    } else {
                        for (superType in type.superTypes) {
                            addSuperinterface(superType.draftClassName)
                        }
                    }
                }
                .apply {
                    for (prop in type.declaredProperties.values) {
                        if (prop.manyToManyViewBaseProp === null) {
                            addProp(prop)
                            addFun(prop)
                            addRefFun(prop)
                            addAssociatedIdProp(prop)
                        }
                    }
                    ProducerGenerator(type, this).generate()
                    if (!type.isMappedSuperclass) {
                        BuilderGenerator(type, this).generate()
                    }
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addProp(prop: ImmutableProp) {
        addProperty(
            PropertySpec
                .builder(
                    prop.name,
                    prop.typeName()
                )
                .addModifiers(KModifier.OVERRIDE)
                .apply {
                    mutable(!prop.isKotlinFormula && prop.manyToManyViewBaseProp === null)
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addFun(prop: ImmutableProp) {
        if ((prop.isAssociation(false) || prop.isList) && prop.manyToManyViewBaseProp == null && !prop.isFormula) {
            addFunction(
                FunSpec
                    .builder(prop.name)
                    .addModifiers(KModifier.ABSTRACT)
                    .returns(prop.typeName(draft = true, overrideNullable = false))
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.addRefFun(prop: ImmutableProp) {
        if (!prop.isAssociation(false) || prop.isList || prop.isFormula) {
            return
        }
        addFunction(
            FunSpec
                .builder(prop.name)
                .addParameter(
                    ParameterSpec
                        .builder(
                            "block",
                            LambdaTypeName.get(
                                prop.typeName(draft = true, overrideNullable = false),
                                emptyList(),
                                UNIT
                            )
                        )
                        .build()
                )
                .addModifiers(KModifier.ABSTRACT)
                .build()
        )
    }

    private fun TypeSpec.Builder.addAssociatedIdProp(prop: ImmutableProp) {
        AssociatedIdGenerator(this, false).generate(prop)
    }

    private fun FileSpec.Builder.addAddFun(type: ImmutableType, withBase: Boolean, withBlock: Boolean) {
        val receiverTypeName = MUTABLE_LIST.parameterizedBy(
            type.draftClassName
        )
        addFunction(
            FunSpec
                .builder("addBy")
                .addAnnotation(generatedAnnotation(type))
                .receiver(receiverTypeName)
                .apply {
                    if (withBase) {
                        addParameter(
                            ParameterSpec
                                .builder(
                                    "base",
                                    type.className.copy(nullable = true)
                                )
                                .build()
                        )
                    }
                    if (withBlock) {
                        addParameter(
                            ParameterSpec
                                .builder(
                                    "block",
                                    LambdaTypeName.get(
                                        type.draftClassName,
                                        emptyList(),
                                        UNIT
                                    )
                                )
                                .build()
                        )
                    }
                }
                .returns(receiverTypeName)
                .addStatement(
                    "add(%T.produce(${produceParams(withBase, withBlock)}) as %T)",
                    type.draftClassName(PRODUCER),
                    type.draftClassName
                )
                .addStatement("return this")
                .build()
        )
    }

    private fun FileSpec.Builder.addNewByFun(
        type: ImmutableType,
        companion: Boolean,
        withBase: Boolean,
        withBlock: Boolean
    ) {
        addFunction(
            FunSpec
                .builder(
                    if (companion) {
                        "invoke"
                    } else {
                        "by"
                    }
                )
                .addAnnotation(generatedAnnotation(type))
                .apply {
                    if (companion) {
                        addModifiers(KModifier.OPERATOR)
                    }
                }
                .receiver(
                    if (companion) {
                        IMMUTABLE_COMPANION_CLASS_NAME
                    } else {
                        IMMUTABLE_CREATOR_CLASS_NAME
                    }.parameterizedBy(type.className)
                )
                .apply {
                    if (withBase) {
                        addParameter(
                            ParameterSpec
                                .builder("base", type.className.copy(nullable = true))
                                .build()
                        )
                    }
                    if (withBlock) {
                        addParameter(
                            ParameterSpec
                                .builder(
                                    "block",
                                    LambdaTypeName.get(
                                        type.draftClassName,
                                        emptyList(),
                                        UNIT
                                    )
                                )
                                .build()
                        )
                    }
                }
                .returns(type.className)
                .addStatement(
                    "return %T.produce(${produceParams(withBase, withBlock)})",
                    type.draftClassName(PRODUCER)
                )
                .build()
        )
    }

    private fun FileSpec.Builder.addCopyFun(type: ImmutableType) {
        addFunction(
            FunSpec
                .builder("copy")
                .addAnnotation(generatedAnnotation(type))
                .receiver(type.className)
                .addParameter(
                    "block",
                    LambdaTypeName.get(
                        type.draftClassName,
                        emptyList(),
                        UNIT
                    )
                )
                .returns(type.className)
                .addCode("return %T.`$`.produce(this, block)", type.draftClassName)
                .build()
        )
    }

    private fun produceParams(withBase: Boolean, withBlock: Boolean) = buildString {
        append(if (withBase) "base" else "null")
        if (withBlock) append(", block")
    }
}