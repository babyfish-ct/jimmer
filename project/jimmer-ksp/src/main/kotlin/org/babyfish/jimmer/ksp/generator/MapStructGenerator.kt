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
        if (prop.isKotlinFormula || prop.idViewBaseProp !== null) {
            return
        }
        if (isMapStructLoadedStateRequired(prop)) {
            addProperty(
                PropertySpec
                    .builder(prop.loadedFieldName!!, BOOLEAN)
                    .mutable(true)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("false")
                    .build()
            )
        }
        addProperty(
            PropertySpec
                .builder(prop.name, prop.typeName().copy(nullable = true))
                .addModifiers(KModifier.PRIVATE)
                .mutable(true)
                .initializer("null")
                .build()
        )
    }

    private fun TypeSpec.Builder.addSetter(prop: ImmutableProp) {
        addFunction(
            FunSpec
                .builder(prop.name)
                .addParameter(prop.name, prop.typeName().copy(nullable = true))
                .returns(type.draftClassName("MapStruct"))
                .apply {
                    val baseProp = prop.idViewBaseProp
                    if (baseProp !== null) {
                        if (isMapStructLoadedStateRequired(baseProp)) {
                            addStatement("this.%N = true", baseProp.loadedFieldName!!)
                        }
                        if (prop.isList) {
                            addStatement(
                                "this.%N = %N?.map { %M(it) } ?: emptyList()",
                                baseProp.name,
                                prop.name,
                                MAKE_ID_ONLY
                            )
                        } else {
                            addStatement(
                                "this.%N = %N?.let { %M(it) }",
                                baseProp.name,
                                prop.name,
                                MAKE_ID_ONLY
                            )
                        }
                    } else {
                        if (isMapStructLoadedStateRequired(prop)) {
                            addStatement("this.%N = true", prop.loadedFieldName!!)
                        }
                        if (prop.isList) {
                            addStatement("this.%L = %L ?: emptyList()", prop.name, prop.name)
                        } else {
                            addStatement("this.%L = %L", prop.name, prop.name)
                        }
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
                            addStatement("val __that = this@%T", type.draftClassName("MapStruct"))
                            for (prop in type.properties.values) {
                                if (prop.isKotlinFormula || prop.idViewBaseProp !== null) {
                                    continue
                                }
                                if (isMapStructLoadedStateRequired(prop)) {
                                    beginControlFlow("if (__that.%L)", prop.loadedFieldName)
                                    addStatement("%L = __that.%L", prop.name, prop.name)
                                    endControlFlow()
                                } else {
                                    beginControlFlow("__that.%L?.let ", prop.name)
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

    companion object {

        @JvmStatic
        private val MAKE_ID_ONLY =
            MemberName("org.babyfish.jimmer.kt", "makeIdOnly")

        @JvmStatic
        private fun isMapStructLoadedStateRequired(prop: ImmutableProp): Boolean =
            prop.isNullable
    }
}