package org.babyfish.jimmer.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.annotation
import org.babyfish.jimmer.ksp.className
import org.babyfish.jimmer.ksp.meta.Context
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType
import org.babyfish.jimmer.sql.Embeddable
import java.io.OutputStreamWriter

class PropsGenerator(
    private val codeGenerator: CodeGenerator,
    private val ctx: Context,
    private val file: KSFile,
    private val modelClassDeclaration: KSClassDeclaration
) {
    fun generate(allFiles: List<KSFile>) {
        val outputFileName =
            file.fileName.let {
                var lastDotIndex = it.lastIndexOf('.')
                if (lastDotIndex == -1) {
                    "${it}$PROPS"
                } else {
                    "${it.substring(0, lastDotIndex)}$PROPS"
                }
            }
        codeGenerator.createNewFile(
            Dependencies(false, *allFiles.toTypedArray()),
            file.packageName.asString(),
            outputFileName
        ).use {
            val fileSpec = FileSpec
                .builder(
                    file.packageName.asString(),
                    outputFileName
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
                    if (modelClassDeclaration.annotation(Embeddable::class) != null) {
                        for (prop in type.properties.values) {
                            addEmbeddableProp(prop, false)
                            addEmbeddableProp(prop, true)
                        }
                    } else {
                        for (prop in type.properties.values) {
                            addProp(type, prop, nonNullTable = true, outerJoin = false)
                            addProp(type, prop, nonNullTable = false, outerJoin = false)
                            addProp(type, prop, nonNullTable = true, outerJoin = true)
                            addProp(type, prop, nonNullTable = false, outerJoin = true)
                        }
                    }
                    if (type.isEntity) {
                        addFetchByFun(type, false)
                        addFetchByFun(type, true)
                    }
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
        if (outerJoin && !prop.isAssociation(true)) {
            return
        }
        if (nonNullTable && (prop.isAssociation(true) || prop.isNullable)) {
            return
        }
        val receiverClassName = when {
            prop.isList -> K_TABLE_EX_CLASS_NAME
            prop.isAssociation(true) || prop.isNullable -> K_PROPS_CLASS_NAME
            nonNullTable -> K_NON_NULL_PROPS_CLASS_NAME
            else -> K_NULLABLE_PROPS_CLASS_NAME
        }.parameterizedBy(
            type.className
        )

        val propName = if (outerJoin) "${prop.name}?" else prop.name
        val innerFunName = when {
            outerJoin -> "outerJoin"
            prop.isAssociation(true) -> "join"
            else -> "get"
        }
        val returnClassName =
            when {
                prop.isAssociation(true) ->
                    if (outerJoin) {
                        K_NULLABLE_TABLE_CLASS_NAME
                    } else {
                        K_NON_NULL_TABLE_CLASS_NAME
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

    private fun FileSpec.Builder.addEmbeddableProp(prop: ImmutableProp, nullable: Boolean) {
        val receiverTypeName = if (nullable) {
            K_NULLABLE_PROP_EXPRESSION.parameterizedBy(
                modelClassDeclaration.className()
            )
        } else {
            K_NON_NULL_PROP_EXPRESSION.parameterizedBy(
                modelClassDeclaration.className()
            )
        }
        val implementorTypeName = if (nullable) {
            K_NULLABLE_PROP_EXPRESSION_IMPLEMENTOR.parameterizedBy(
                modelClassDeclaration.className()
            )
        } else {
            K_NON_NULL_PROP_EXPRESSION_IMPLEMENTOR.parameterizedBy(
                modelClassDeclaration.className()
            )
        }
        val returnTypeName = if (nullable) {
            K_NULLABLE_PROP_EXPRESSION.parameterizedBy(
                prop.typeName()
            )
        } else {
            K_NON_NULL_PROP_EXPRESSION.parameterizedBy(
                prop.typeName()
            )
        }
        addProperty(
            PropertySpec
                .builder(prop.name, returnTypeName)
                .receiver(receiverTypeName)
                .getter(
                    FunSpec
                        .getterBuilder()
                        .addStatement(
                            "return (this as %T).get(%T::%L)",
                            implementorTypeName,
                            modelClassDeclaration.className(),
                            prop.name
                        )
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