package org.babyfish.jimmer.ksp.dto

import com.squareup.kotlinpoet.*
import org.babyfish.jimmer.dto.compiler.AbstractProp
import org.babyfish.jimmer.dto.compiler.Anno.TypeRefValue
import org.babyfish.jimmer.dto.compiler.DtoModifier
import org.babyfish.jimmer.dto.compiler.DtoProp
import org.babyfish.jimmer.dto.compiler.DtoType
import org.babyfish.jimmer.ksp.immutable.generator.INPUT_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.JSON_NAMING_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.JSON_POJO_BUILDER_CLASS_NAME
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
                    addAnnotations()
                    addMembers()
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addAnnotations() {
        addAnnotation(
            AnnotationSpec
                .builder(JSON_POJO_BUILDER_CLASS_NAME)
                .addMember("withPrefix = %S", "")
                .build()
        )

        for (annotation in dtoType.annotations) {
            if (annotation.qualifiedName == JSON_NAMING_CLASS_NAME.canonicalName) {
                if (!annotation.valueMap.containsKey("value")) {
                    continue
                }
                addAnnotation(
                    AnnotationSpec
                        .builder(JSON_NAMING_CLASS_NAME)
                        .addMember(
                            "value = %T::class",
                            ClassName.bestGuess((annotation.valueMap["value"] as TypeRefValue).typeRef.typeName)
                        )
                        .build()
                )
            }
        }
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
        parentGenerator.statePropName(prop, true)?.let {
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
                .builder(prop.name)
                .addParameter(prop.name, typeName)
                .returns(parentGenerator.getDtoClassName("Builder"))
                .addStatement("this.%L = %L", prop.name, prop.name)
                .apply {
                    parentGenerator.statePropName(prop, true)?.let {
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
                .addCode(
                    CodeBlock
                        .builder()
                        .add("return %T(\n", parentGenerator.getDtoClassName())
                        .indent()
                        .apply {
                            for (prop in dtoType.dtoProps) {
                                val builderStatePropName = parentGenerator.statePropName(prop, true)
                                val dtoStatePropName = parentGenerator.statePropName(prop, false)
                                if (builderStatePropName === null) {
                                    addArg(prop)
                                    add(",\n")
                                } else {
                                    add("// %L\n", prop.inputModifier)
                                    if (prop.inputModifier == DtoModifier.FIXED) {
                                        beginControlFlow("if (!%L)", builderStatePropName)
                                        add(
                                            "throw %T.unknownNullableProperty(%T::class.java, %S)",
                                            INPUT_CLASS_NAME,
                                            parentGenerator.getDtoClassName(),
                                            prop.name
                                        )
                                        nextControlFlow("else")
                                        addArg(prop)
                                        endControlFlow()
                                        add(",\n")
                                    } else {
                                        addArg(prop)
                                        add(",\n")
                                    }
                                    if (dtoStatePropName !== null) {
                                        add("%L,\n", builderStatePropName)
                                    }
                                }
                            }
                            for (prop in dtoType.userProps) {
                                addArg(prop)
                                add(",\n")
                            }
                        }
                        .unindent()
                        .add(")\n")
                        .build()
                )
                .build()
        )
    }

    private fun CodeBlock.Builder.addArg(prop: AbstractProp) {
        add(prop.name)
        if (!prop.isNullable) {
            add(
                " ?: throw %T.unknownNonNullProperty(%T::class.java, %S)",
                INPUT_CLASS_NAME,
                parentGenerator.getDtoClassName(),
                prop.name
            )
        }
    }

    companion object {

        private fun isFieldNullable(prop: AbstractProp): Boolean =
            prop !is DtoProp<*, *> || (prop.funcName != "null" && prop.funcName != "notNull")
    }
}