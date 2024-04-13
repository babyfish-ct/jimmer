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
        stateFieldName(prop)?.let {
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
                    stateFieldName(prop)?.let {
                        addStatement("this.%L = true", it)
                    }
                }
                .addStatement("return this")
                .build()
        )
    }

    private fun TypeSpec.Builder.addBuild() {
        val isDynamic = dtoType.modifiers.contains(DtoTypeModifier.DYNAMIC)
        addFunction(
            FunSpec
                .builder("build")
                .returns(parentGenerator.getDtoClassName())
                .addStatement("val _input = %T()", parentGenerator.getDtoClassName())
                .apply {
                    for (prop in dtoType.dtoProps) {
                        val stateFieldName = stateFieldName(prop)
                        addStatement("val %L = this.%L", prop.name, prop.name)
                        if (prop.isNullable) {
                            if (isDynamic) {
                                beginControlFlow(
                                    "if (%L)",
                                    if (stateFieldName !== null) stateFieldName else "${prop.name} !== null"
                                )
                                addStatement("_input.%L = %L", prop.name, prop.name)
                                endControlFlow()
                            } else {
                                beginControlFlow(
                                    "if (%L)",
                                    if (stateFieldName !== null) "!${stateFieldName}" else "${prop.name} === null"
                                )
                                addStatement(
                                    "throw %T.unknownNullableProperty(%T::class.java, %S)",
                                    INPUT_CLASS_NAME,
                                    parentGenerator.getDtoClassName(),
                                    prop.name
                                )
                                endControlFlow()
                                addStatement("_input.%L = %L", prop.name, prop.name)
                            }
                        } else {
                            beginControlFlow(
                                "if (%L)",
                                if (stateFieldName !== null) "!${stateFieldName}" else "${prop.name} === null"
                            )
                            addStatement(
                                "throw %T.unknownNonNullProperty(%T::class.java, %S)",
                                INPUT_CLASS_NAME,
                                parentGenerator.getDtoClassName(),
                                prop.name
                            )
                            endControlFlow()
                            addStatement("_input.%L = %L", prop.name, prop.name)
                        }
                    }
                }
                .addStatement("return _input")
                .build()
        )
    }

    private fun stateFieldName(prop: AbstractProp): String? =
        if (prop.isNullable) {
            StringUtil.identifier("_is", prop.name, "Loaded")
        } else {
            null
        }

    companion object {

        private fun isFieldNullable(prop: AbstractProp): Boolean =
            prop !is DtoProp<*, *> || (prop.funcName != "null" && prop.funcName != "notNull")
    }
}