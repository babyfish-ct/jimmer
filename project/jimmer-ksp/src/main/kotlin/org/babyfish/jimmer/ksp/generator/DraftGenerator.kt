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
                    "${it}$DRAFT_SUFFIX"
                } else {
                    "${it.substring(0, lastDotIndex)}$DRAFT_SUFFIX"
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
                        addType(ctx.typeOf(classDeclaration))
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
                .interfaceBuilder("${type.simpleName}${DRAFT_SUFFIX}")
                .apply {
                    type.superType?.let {
                        addSuperinterface(it.draftClassName)
                    }
                }
                .addSuperinterface(type.className)
                .apply {
                    for (prop in type.declaredProperties.values) {
                        addProp(prop)
                        addFun(prop)
                    }
                    ProducerGenerator(type, this).generate()
                }
                .build()
        )
        addAddFun(type)
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
        if (prop.isAssociation) {
            addFunction(
                FunSpec
                    .builder(prop.name)
                    .addModifiers(KModifier.ABSTRACT)
                    .returns(prop.typeName(true))
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
                    type.draftClassName("Producer"),
                    type.draftClassName
                )
                .addStatement("return this")
                .build()
        )
    }
}