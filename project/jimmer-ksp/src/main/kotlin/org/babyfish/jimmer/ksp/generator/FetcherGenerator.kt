package org.babyfish.jimmer.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.meta.Context
import org.babyfish.jimmer.ksp.meta.ImmutableType
import java.io.OutputStreamWriter

class FetcherGenerator(
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
                    "${it}$FETCHER"
                } else {
                    "${it.substring(0, lastDotIndex)}$FETCHER"
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
                    addCreateFun(type, false)
                    addCreateFun(type, true)
                    FetcherDslGenerator(type, this).generate()
                    addEmptyFetcher(type)
                }.build()
            val writer = OutputStreamWriter(it, Charsets.UTF_8)
            fileSpec.writeTo(writer)
            writer.flush()
        }
    }

    private fun FileSpec.Builder.addCreateFun(type: ImmutableType, withBase: Boolean) {
        addFunction(
            FunSpec
                .builder("by")
                .receiver(
                    FETCHER_CREATOR_CLASS_NAME.parameterizedBy(
                        type.className
                    )
                )
                .apply {
                    if (withBase) {
                        addParameter(
                            "base",
                            FETCHER_CLASS_NAME.parameterizedBy(
                                type.className
                            ).copy(nullable = true)
                        )
                    }
                }
                .addParameter(
                    "block",
                    LambdaTypeName.get(
                        type.fetcherDslClassName,
                        emptyList(),
                        UNIT
                    )
                )
                .returns(
                    FETCHER_CLASS_NAME.parameterizedBy(
                        type.className
                    )
                )
                .addStatement("val dsl = %T(%Lempty${type.simpleName}$FETCHER)", type.fetcherDslClassName, if (withBase) "base ?: " else "")
                .addStatement("dsl.block()")
                .addStatement("return dsl.internallyGetFetcher()")
                .build()
        )
    }

    private fun FileSpec.Builder.addEmptyFetcher(type: ImmutableType) {
        addProperty(
            PropertySpec
                .builder(
                    "empty${type.simpleName}$FETCHER",
                    FETCHER_CLASS_NAME.parameterizedBy(
                        type.className
                    ),
                    KModifier.PRIVATE
                )
                .initializer(
                    "%T(%T::class.java)",
                    FETCHER_IMPL_CLASS_NAME,
                    type.className
                )
                .build()
        )
    }
}