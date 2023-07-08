package org.babyfish.jimmer.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.generator.DYNAMIC
import org.babyfish.jimmer.ksp.meta.Context
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType
import java.io.OutputStreamWriter

class DynamicGenerator(
    private val codeGenerator: CodeGenerator,
    private val ctx: Context,
    private val file: KSFile,
    private val modelClassDeclarations: List<KSClassDeclaration>
) {

    fun generate(allFiles: List<KSFile>) {
        val draftFileName =
            file.fileName.let {
                val lastDotIndex = it.lastIndexOf('.')
                if (lastDotIndex == -1) {
                    "$DYNAMIC$it"
                } else {
                    "$DYNAMIC${it.substring(0, lastDotIndex)}"
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
                    for (classDeclaration in modelClassDeclarations) {
                        val type = ctx.typeOf(classDeclaration)
                        addType(type)
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
                .classBuilder("$DYNAMIC${type.simpleName}")
                .addModifiers(KModifier.DATA)
                .addSuperinterface(
                    DYNAMIC_CLASS_NAME.parameterizedBy(
                        type.className
                    )
                )
                .apply {
                    addField(type)
                    addConstructor(type)
                    for (prop in type.properties.values) {
                        addProp(prop)
                    }
                    addUnwrap()
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addField(type: ImmutableType) {
        addProperty(
            PropertySpec
                .builder(
                    "raw",
                    type.className
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer("raw")
                .build()
        )
    }

    private fun TypeSpec.Builder.addConstructor(type: ImmutableType) {
        primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("raw", type.className)
                .build()
        )
    }

    private fun TypeSpec.Builder.addProp(prop: ImmutableProp) {
        addProperty(
            PropertySpec
                .builder(
                    prop.name,
                    prop.dynamicTypeName
                )
                .getter(
                    FunSpec
                        .getterBuilder()
                        .apply {
                            if (prop.declaringType.isMappedSuperclass) {
                                beginControlFlow(
                                    "if ((raw as %T).__isLoaded(%S))",
                                    IMMUTABLE_SPI_CLASS_NAME,
                                    prop.name
                                )
                            } else {
                                beginControlFlow(
                                    "if ((raw as %T).__isLoaded(%T.byIndex(%T.%L)))",
                                    IMMUTABLE_SPI_CLASS_NAME,
                                    PROP_ID_CLASS_NAME,
                                    prop.declaringType.draftClassName("$"),
                                    prop.slotName
                                )
                            }
                            addStatement("return null")
                            endControlFlow()
                            addCode("return raw.%L", prop.name)
                            if (prop.isAssociation(false)) {
                                addCode(if (prop.isNullable) "?." else  ".")
                                addCode(if (prop.isList) "map" else "let")
                                addCode(" { %T(it) }", prop.targetType!!.dynamicClassName)
                            }
                        }
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addUnwrap() {
        addFunction(
            FunSpec
                .builder("__unwrap")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("return raw")
                .build()
        )
    }
}