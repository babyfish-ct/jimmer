package org.babyfish.jimmer.ksp.immutable.generator

import com.squareup.kotlinpoet.*
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType
import org.babyfish.jimmer.ksp.util.generatedAnnotation

class BuilderGenerator(
    private val type: ImmutableType,
    private val parent: TypeSpec.Builder
) {
    fun generate() {
        parent.addType(
            TypeSpec
                .classBuilder("Builder")
                .addAnnotation(generatedAnnotation(type))
                .apply {
                    addMembers()
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addMembers() {
        addField()
        addConstructor()
        addDefaultConstructor()
        for (prop in type.properties.values) {
            if (!prop.isKotlinFormula && prop.manyToManyViewBaseProp === null) {
                addSetter(prop)
            }
        }
        addBuildFun()
    }

    private fun TypeSpec.Builder.addField() {
        addProperty(
            PropertySpec
                .builder("__draft", type.draftClassName("$", "DraftImpl"))
                .addModifiers(KModifier.PRIVATE)
                .build()
        )
    }

    private fun TypeSpec.Builder.addConstructor() {
        addFunction(
            FunSpec
                .constructorBuilder()
                .addParameter("base", type.className.copy(nullable = true))
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            addStatement(
                                "__draft = %T(null, base)",
                                type.draftClassName("$", "DraftImpl")
                            )
                            val props = type.properties.values.filter { isVisibilityControllable(it) }
                            for (prop in props) {
                                addStatement(
                                    "__draft.__show(%T.byIndex(%T.%L), false)",
                                    PROP_ID_CLASS_NAME,
                                    type.draftClassName("$"),
                                    prop.slotName
                                )
                            }
                        }
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addDefaultConstructor() {
        addFunction(
            FunSpec
                .constructorBuilder()
                .callThisConstructor("null")
                .build()
        )
    }

    private fun TypeSpec.Builder.addSetter(prop: ImmutableProp) {
        addFunction(
            FunSpec
                .builder(prop.name)
                .copyNonJimmerMethodAnnotations(prop)
                .addParameter(prop.name, prop.typeName().copy(nullable = true))
                .returns(type.draftClassName("Builder"))
                .apply {
                    if (prop.isNullable) {
                        addStatement("__draft.%L = %L", prop.name, prop.name)
                        addStatement(
                            "__draft.__show(%T.byIndex(%T.%L), true)",
                            PROP_ID_CLASS_NAME,
                            type.draftClassName("$"),
                            prop.slotName
                        )
                    } else {
                        beginControlFlow("if (%L !== null)", prop.name)
                        addStatement("__draft.%L = %L", prop.name, prop.name)
                        addStatement(
                            "__draft.__show(%T.byIndex(%T.%L), true)",
                            PROP_ID_CLASS_NAME,
                            type.draftClassName("$"),
                            prop.slotName
                        )
                        endControlFlow()
                    }
                }
                .addStatement("return this")
                .build()
        )
    }

    private fun TypeSpec.Builder.addBuildFun() {
        addFunction(
            FunSpec
                .builder("build")
                .returns(type.className)
                .addCode(
                    CodeBlock
                        .builder()
                        .addStatement(
                            "return __draft.__unwrap() as %T",
                            type.className
                        )
                        .build()
                )
                .build()
        )
    }

    companion object {

        private fun isVisibilityControllable(prop: ImmutableProp): Boolean {
            return prop.isBaseProp ||
                prop.dependencies.isNotEmpty() ||
                prop.idViewBaseProp !== null ||
                prop.manyToManyViewBaseProp !== null
        }
    }
}