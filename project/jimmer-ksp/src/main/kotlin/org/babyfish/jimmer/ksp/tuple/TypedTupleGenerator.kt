package org.babyfish.jimmer.ksp.tuple

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import org.babyfish.jimmer.impl.util.StringUtil
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.MetaException
import org.babyfish.jimmer.ksp.immutable.generator.COLLECTIONS_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.SELECTION_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.TUPLE_MAPPER_CLASS_NAME
import org.babyfish.jimmer.ksp.name
import java.io.OutputStreamWriter

class TypedTupleGenerator(
    val ctx: Context,
    val declaration: KSClassDeclaration
) {
    private val props = declaration.getDeclaredProperties().toList()

    init {
        if (props.isEmpty()) {
            throw MetaException(
                declaration,
                "There is properties"
            )
        }
    }

    fun generate() {
        val allFiles = ctx.resolver.getAllFiles().toList()
        ctx.environment.codeGenerator.createNewFile(
            Dependencies(false, *allFiles.toTypedArray()),
            declaration.packageName.asString(),
            "${declaration.simpleName.asString()}Mapper"
        ).use {
            val fileSpec = FileSpec
                .builder(
                    declaration.packageName.asString(),
                    "${declaration.simpleName.asString()}Mapper"
                ).apply {
                    indent("    ")
                    addType(
                        TypeSpec
                            .classBuilder("${declaration.simpleName.asString()}Mapper")
                            .addSuperinterface(
                                TUPLE_MAPPER_CLASS_NAME.parameterizedBy(
                                    declaration.toClassName()
                                )
                            )
                            .apply {
                                addMembers()
                            }
                            .build()
                    )
                }.build()
            val writer = OutputStreamWriter(it, Charsets.UTF_8)
            fileSpec.writeTo(writer)
            writer.flush()
        }
    }

    private fun TypeSpec.Builder.addMembers() {
        addConstructor()
        addProp()
        addGetSelections()
        addCreateTuple()
        for (i in 1 until props.size) {
            addBuilderClass(i)
        }
        addCompanion()
    }

    private fun TypeSpec.Builder.addConstructor() {
        primaryConstructor(
            FunSpec.constructorBuilder()
                .addModifiers(KModifier.PRIVATE)
                .addParameter(
                    "selections",
                    SELECTIONS_FIELD_TYPE
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addProp() {
        addProperty(
            PropertySpec
                .builder(
                    "selections",
                    SELECTIONS_FIELD_TYPE
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer("selections")
                .build()
        )
    }

    private fun TypeSpec.Builder.addGetSelections() {
        addFunction(
            FunSpec
                .builder("getSelections")
                .addModifiers(KModifier.OVERRIDE)
                .addAnnotation(
                    AnnotationSpec
                        .builder(Suppress::class)
                        .addMember("%S", "UNCHECKED_CAST")
                        .build()
                )
                .returns(
                    LIST.parameterizedBy(
                        SELECTION_CLASS_NAME.parameterizedBy(STAR)
                    )
                )
                .addStatement(
                    "return %T.unmodifiableList(listOf(*selections as Array<%T>))",
                    COLLECTIONS_CLASS_NAME,
                    SELECTION_CLASS_NAME.parameterizedBy(STAR)
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addCreateTuple() {
        addFunction(
            FunSpec
                .builder("createTuple")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("args", ARRAY.parameterizedBy(ANY.copy(nullable = true)))
                .returns(declaration.toClassName())
                .addCode(
                    CodeBlock.builder().apply {
                        add("return %T(\n", declaration.toClassName())
                        indent()
                        for (i in props.indices) {
                            if (i != 0) {
                                add(",\n")
                            }
                            add(
                                "%L = args[%L] as %T",
                                props[i].name,
                                i.toString(),
                                props[i].type.toTypeName())
                        }
                        unindent()
                        add("\n)\n")
                    }.build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addBuilderClass(index: Int) {
        val prop = props[index]
        addType(
            TypeSpec
                .classBuilder(StringUtil.typeName(prop.name, "Builder"))
                .primaryConstructor(
                    FunSpec
                        .constructorBuilder()
                        .addModifiers(KModifier.INTERNAL)
                        .addParameter("selections", SELECTIONS_FIELD_TYPE)
                        .build()
                )
                .addProperty(
                    PropertySpec
                        .builder("selections", SELECTIONS_FIELD_TYPE)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("selections")
                        .build()
                )
                .addFunction(
                    FunSpec
                        .builder(prop.name)
                        .addParameter(
                            "selection",
                            SELECTION_CLASS_NAME.parameterizedBy(
                                prop.type.toTypeName()
                            )
                        )
                        .returns(buildReturnTypeName(index))
                        .addStatement("selections[%L] = selection", index.toString())
                        .addStatement("return %T(selections)", buildReturnTypeName(index))
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addCompanion() {
        addType(
            TypeSpec
                .companionObjectBuilder()
                .addFunction(
                    FunSpec
                        .builder(props[0].name)
                        .addParameter(
                            "selection",
                            SELECTION_CLASS_NAME.parameterizedBy(
                                props[0].type.toTypeName()
                            )
                        )
                        .returns(buildReturnTypeName(0))
                        .addStatement(
                            "val selections = arrayOfNulls<%T>(%L)",
                            SELECTION_CLASS_NAME.parameterizedBy(STAR),
                            props.size
                        )
                        .addStatement("selections[0] = selection")
                        .addStatement("return %T(selections)", buildReturnTypeName(0))
                        .build()
                )
                .build()
        )
    }

    private fun buildReturnTypeName(index: Int): ClassName {
        if (index + 1 < props.size) {
            return ClassName(
                declaration.packageName.asString(),
                StringUtil.typeName(declaration.simpleName.asString(), "Mapper"),
                StringUtil.typeName(props[index + 1].name, "Builder")
            )
        }
        return ClassName(
            declaration.packageName.asString(),
            StringUtil.typeName(declaration.simpleName.asString(), "Mapper")
        )
    }

    companion object {
        private val SELECTIONS_FIELD_TYPE =
            ARRAY.parameterizedBy(
                SELECTION_CLASS_NAME.parameterizedBy(
                    STAR
                ).copy(nullable = true)
            )
    }
}