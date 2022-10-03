package org.babyfish.jimmer.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.className
import org.babyfish.jimmer.ksp.meta.Context
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType
import java.io.OutputStreamWriter

class TableGenerator(
    private val codeGenerator: CodeGenerator,
    private val ctx: Context,
    private val file: KSFile,
    private val modelClassDeclaration: KSClassDeclaration
) {
    fun generate(allFiles: List<KSFile>) {
        val draftFileName =
            file.fileName.let {
                var lastDotIndex = it.lastIndexOf('.')
                if (lastDotIndex == -1) {
                    "${it}$TABLE"
                } else {
                    "${it.substring(0, lastDotIndex)}$TABLE"
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
                    val type = ctx.typeOf(modelClassDeclaration)
                    for (prop in type.properties.values) {
                        addProp(type, prop, nonNullTable = false, outerJoin = false)
                        addProp(type, prop, nonNullTable = true, outerJoin = false)
                        addProp(type, prop, nonNullTable = false, outerJoin = true)
                        addProp(type, prop, nonNullTable = true, outerJoin = true)
                    }
                    addFetchByFun(type, false)
                    addFetchByFun(type, true)
                }.build()
            val writer = OutputStreamWriter(it, Charsets.UTF_8)
            fileSpec.writeTo(writer)
            writer.flush()
        }
    }

    private fun FileSpec.Builder.addProp(
        type: ImmutableType,
        prop: ImmutableProp,
        nonNullTable: Boolean,
        outerJoin: Boolean
    ) {
        if (prop.isTransient) {
            return
        }

        if (outerJoin && !prop.isAssociation) {
            return
        }
        if (nonNullTable && (
                (prop.isAssociation && outerJoin) ||
                    (!prop.isAssociation && prop.isNullable)
            )
        ) {
            return
        }

        val receiverClassName = if (prop.isList) {
            when {
                outerJoin -> K_TABLE_EX_CLASS_NAME
                nonNullTable -> K_NON_NULL_TABLE_EX_CLASS_NAME
                prop.isAssociation -> K_NULLABLE_TABLE_EX_CLASS_NAME
                else -> K_TABLE_CLASS_NAME
            }
        } else {
            when {
                outerJoin -> K_TABLE_CLASS_NAME
                nonNullTable -> K_NON_NULL_TABLE_CLASS_NAME
                prop.isAssociation -> K_NULLABLE_TABLE_CLASS_NAME
                else -> K_TABLE_CLASS_NAME
            }
        }.parameterizedBy(
            type.className
        )

        val propName = if (outerJoin) "${prop.name}?" else prop.name
        val innerFunName = when {
            outerJoin -> "outerJoin"
            prop.isAssociation -> "join"
            else -> "get"
        }
        val returnClassName =
            when {
                prop.isList ->
                    if (nonNullTable) {
                        K_NON_NULL_TABLE_EX_CLASS_NAME
                    } else {
                        K_NULLABLE_TABLE_EX_CLASS_NAME
                    }
                prop.isAssociation ->
                    if (nonNullTable) {
                        K_NON_NULL_TABLE_CLASS_NAME
                    } else {
                        K_NULLABLE_TABLE_CLASS_NAME
                    }
                else ->
                    if (nonNullTable) {
                        K_NON_NULL_PROP_EXPRESSION
                    } else {
                        K_NULLABLE_PROP_EXPRESSION
                    }
            }.parameterizedBy(
                prop.targetTypeName(overrideNullable = false)
            )

        addProperty(
            PropertySpec
                .builder(propName, returnClassName)
                .receiver(receiverClassName)
                .getter(
                    FunSpec
                        .getterBuilder()
                        .addCode("return %L(%S)", innerFunName, prop.name)
                        .build()
                )
                .build()
        )
    }

    private fun FileSpec.Builder.addFetchByFun(type: ImmutableType, nullable: Boolean) {
        addFunction(
            FunSpec
                .builder("fetchBy")
                .receiver(
                    if (nullable) {
                        K_NULLABLE_TABLE_CLASS_NAME
                    } else {
                        K_NON_NULL_TABLE_CLASS_NAME
                    }
                    .parameterizedBy(type.className)
                )
                .addParameter(
                    "block",
                    LambdaTypeName.get(
                        type.fetcherDslClassName,
                        emptyList(),
                        UNIT
                    )
                )
                .returns(
                    SELECTION_CLASS_NAME.parameterizedBy(
                        type.className.copy(nullable = nullable)
                    )
                )
                .addCode(
                    "return fetch(%T(%T::class).by(block))",
                    NEW_FETCHER_FUN_CLASS_NAME,
                    type.className
                )
                .build()
        )
    }
}