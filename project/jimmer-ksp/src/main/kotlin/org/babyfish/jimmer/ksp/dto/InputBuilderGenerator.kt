package org.babyfish.jimmer.ksp.dto

import com.squareup.kotlinpoet.*
import org.babyfish.jimmer.dto.compiler.*
import org.babyfish.jimmer.impl.util.StringUtil
import org.babyfish.jimmer.ksp.immutable.generator.INPUT_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType

class InputBuilderGenerator(
    private val parentGenerator: DtoGenerator
) {

    private val dtoType: DtoType<ImmutableType, ImmutableProp> = parentGenerator.dtoType

    fun generate() {
        parentGenerator.typeBuilder.addType(
            TypeSpec
                .classBuilder("Builder")
                .apply {
                    addMembers()
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addMembers() {
        for (prop in dtoType.dtoProps) {
            addField(prop)
            addStateField(prop)
        }
        for (prop in dtoType.userProps) {
            addField(prop)
            addStateField(prop)
        }
        for (prop in dtoType.dtoProps) {
            addSetter(prop)
        }
        for (prop in dtoType.userProps) {
            addSetter(prop)
        }
        addBuild()
    }

    private fun TypeSpec.Builder.addField(prop: AbstractProp) {
        val isFieldNullable = isFieldNullable(prop)
        val typeName = parentGenerator.propTypeName(prop).copy(nullable = isFieldNullable)
        addProperty(
            PropertySpec
                .builder(prop.name, typeName)
                .addModifiers(KModifier.PRIVATE)
                .mutable(true)
                .initializer(
                    if (isFieldNullable) {
                        "null"
                    } else {
                        "false"
                    }
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addStateField(prop: AbstractProp) {
        parentGenerator.statePropName(prop)?.let {
            addProperty(
                PropertySpec
                    .builder(it, BOOLEAN, KModifier.PRIVATE)
                    .mutable(true)
                    .initializer("false")
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.addSetter(prop: AbstractProp) {
        val typeName = parentGenerator.propTypeName(prop)
        addFunction(
            FunSpec
                .builder(StringUtil.identifier("with", prop.name))
                .addParameter(prop.name, typeName)
                .returns(parentGenerator.getDtoClassName("Builder"))
                .addStatement("this.%L = %L", prop.name, prop.name)
                .apply {
                    parentGenerator.statePropName(prop)?.let {
                        addStatement("this.%L = true", it)
                    }
                }
                .addStatement("return this")
                .build()
        )
    }

    private fun TypeSpec.Builder.addBuild() {
        addFunction(
            FunSpec
                .builder("build")
                .returns(parentGenerator.getDtoClassName())
                .addStatement("val _input = %T()", parentGenerator.getDtoClassName())
                .apply {
                    for (prop in dtoType.dtoProps) {
                        if (!prop.isNullable) {
                            addStatement(
                                "_input.%L = %L ?: throw %T.%L(%T::class.java, %S)",
                                prop.name,
                                prop.name,
                                INPUT_CLASS_NAME,
                                "unknownNonNullProperty",
                                parentGenerator.getDtoClassName(),
                                prop.getName()
                            )
                        } else {
                            val statePropName = parentGenerator.statePropName(prop)
                            when (prop.inputModifier) {
                                DtoModifier.FIXED -> {
                                    beginControlFlow("if (!%L)", statePropName!!)
                                    addStatement(
                                        "throw %T.%L(%T::class.java, %S)",
                                        INPUT_CLASS_NAME,
                                        "unknownNullableProperty",
                                        parentGenerator.getDtoClassName(),
                                        prop.getName()
                                    );
                                    endControlFlow()
                                    addStatement("_input.%L = %L", prop.name, prop.name)
                                }
                                DtoModifier.STATIC -> {
                                    addStatement("_input.%L = %L", prop.name, prop.name)
                                }
                                DtoModifier.DYNAMIC -> {
                                    beginControlFlow("if (%L)", statePropName!!)
                                    addStatement("_input.%L = %L", prop.name, prop.name)
                                    endControlFlow()
                                }
                                DtoModifier.FUZZY -> {
                                    beginControlFlow("if (%L !== null)", prop.name)
                                    addStatement("_input.%L = %L", prop.name, prop.name)
                                    endControlFlow()
                                }
                                else -> {}
                            }
                        }
                    }
                    for (prop in dtoType.userProps) {
                        addStatement("_input.%L = %L", prop.name, prop.name)
                    }
                }
                .addStatement("return _input")
                .build()
        )
    }

    companion object {

        private fun isFieldNullable(prop: AbstractProp): Boolean =
            prop !is DtoProp<*, *> || (prop.funcName != "null" && prop.funcName != "notNull")
    }
}