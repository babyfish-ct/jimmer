package org.babyfish.jimmer.ksp.generator

import com.squareup.kotlinpoet.*
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType

class MapStructGenerator(
    private val type: ImmutableType,
    private val parent: TypeSpec.Builder
) {
    fun generate() {
        parent.addType(
            TypeSpec
                .classBuilder("MapStruct")
                .apply {
                    addMembers()
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addMembers() {
        for (prop in type.properties.values) {
            if (!prop.isKotlinFormula) {
                addFields(prop)
            }
        }
        for (prop in type.properties.values) {
            if (!prop.isKotlinFormula) {
                addSetter(prop)
            }
        }
        addBuild()
    }

    private fun TypeSpec.Builder.addFields(prop: ImmutableProp) {
        prop.loadedFieldName?.let {
            addProperty(
                PropertySpec
                    .builder(it, BOOLEAN)
                    .mutable(true)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("false")
                    .build()
            )
        }
        addProperty(
            PropertySpec
                .builder(prop.name, prop.typeName().copy(nullable = !prop.isPrimitive))
                .addModifiers(KModifier.PRIVATE)
                .mutable(true)
                .initializer(
                    if (prop.isPrimitive) {
                        when (prop.typeName()) {
                            BOOLEAN -> "false"
                            CHAR -> "Char.MIN_VALUE"
                            FLOAT -> "0F"
                            DOUBLE -> "0.0"
                            else -> "0"
                        }
                    } else {
                        "null"
                    }
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addSetter(prop: ImmutableProp) {
        addFunction(
            FunSpec
                .builder(prop.name)
                .addParameter(prop.name, prop.typeName().copy(nullable = !prop.isPrimitive))
                .returns(type.draftClassName("MapStruct"))
                .apply {
                    if (prop.isList) {
                        addStatement("this.%L = %L ?: emptyList()", prop.name, prop.name)
                    } else if (prop.isNullable || prop.isPrimitive) {
                        prop.loadedFieldName?.let {
                            addStatement("this.%L = true", it)
                        }
                        addStatement("this.%L = %L", prop.name, prop.name)
                    } else {
                        beginControlFlow("if (%L !== null)", prop.name)
                        prop.loadedFieldName?.let {
                            addStatement("this.%L = true", it)
                        }
                        addStatement("this.%L = %L", prop.name, prop.name)
                        endControlFlow()
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
                .returns(type.className)
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            add("return %T\n.%L.produce {", type.draftClassName, "`$`")
                            indent()
                            addStatement("val that = this@%T", type.draftClassName("MapStruct"))
                            for (prop in type.properties.values) {
                                if (prop.isKotlinFormula) {
                                    continue
                                }
                                val loadName = prop.loadedFieldName
                                if (loadName !== null) {
                                    beginControlFlow("if (that.%L)", loadName)
                                    addStatement("%L = that.%L", prop.name, prop.name)
                                    endControlFlow()
                                } else {
                                    beginControlFlow("that.%L?.let ", prop.name)
                                    addStatement("%L = it", prop.name)
                                    endControlFlow()
                                }
                            }
                            unindent()
                            add("}\n")
                        }
                        .build()
                )
                .build()
        )
    }
}