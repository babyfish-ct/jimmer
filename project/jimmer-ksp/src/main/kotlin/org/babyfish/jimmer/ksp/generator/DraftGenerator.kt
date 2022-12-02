package org.babyfish.jimmer.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.meta.Context
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType
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
                        addNewByFun(type)
                        addAddFun(type)
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
                .interfaceBuilder("${type.simpleName}${DRAFT}")
                .addAnnotation(DSL_SCOPE_CLASS_NAME)
                .addSuperinterface(type.className)
                .apply {
                    type.superType?.let {
                        addSuperinterface(it.draftClassName)
                    } ?: addSuperinterface(DRAFT_CLASS_NAME)
                }
                .apply {
                    for (prop in type.declaredProperties.values) {
                        addProp(prop)
                        addFun(prop)
                    }
                    ProducerGenerator(type, this).generate()
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
                .apply {
                    addModifiers(KModifier.OVERRIDE)
                    mutable(true)
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addFun(prop: ImmutableProp) {
        if (prop.isAssociation(false) || prop.isList) {
            addFunction(
                FunSpec
                    .builder(prop.name)
                    .addModifiers(KModifier.ABSTRACT)
                    .returns(prop.typeName(draft = true, overrideNullable = false))
                    .build()
            )
        }
    }

    private fun FileSpec.Builder.addAddFun(type: ImmutableType) {
        val receiverTypeName = MUTABLE_LIST.parameterizedBy(
            type.draftClassName
        )
        addFunction(
            FunSpec
                .builder("addBy")
                .receiver(receiverTypeName)
                .addParameter(
                    ParameterSpec
                        .builder(
                            "base",
                            type.className.copy(nullable = true)
                        )
                        .defaultValue("null")
                        .build()
                )
                .addParameter(
                    "block",
                    LambdaTypeName.get(
                        type.draftClassName,
                        emptyList(),
                        UNIT
                    )
                )
                .returns(receiverTypeName)
                .addStatement(
                    "add(%T.produce(base, block) as %T)",
                    type.draftClassName(PRODUCER),
                    type.draftClassName
                )
                .addStatement("return this")
                .build()
        )
    }

    private fun FileSpec.Builder.addNewByFun(type: ImmutableType) {
        addFunction(
            FunSpec
                .builder("by")
                .receiver(
                    IMMUTABLE_CREATOR_CLASS_NAME
                        .parameterizedBy(type.className)
                )
                .addParameter(
                    ParameterSpec
                        .builder("base", type.className.copy(nullable = true))
                        .defaultValue("null")
                        .build()
                )
                .addParameter(
                    "block",
                    LambdaTypeName.get(
                        type.draftClassName,
                        emptyList(),
                        UNIT
                    )
                )
                .returns(type.className)
                .addStatement(
                    "return %T.produce(base, block)",
                    type.draftClassName(PRODUCER)
                )
                .build()
        )
    }
}