package org.babyfish.jimmer.ksp.immutable.generator

import com.squareup.kotlinpoet.*
import org.babyfish.jimmer.ksp.util.addGeneratedAnnotation
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType

class BuilderGenerator(
    private val type: ImmutableType,
    private val parent: TypeSpec.Builder
) {
    fun generate() {
        parent.addType(
            TypeSpec
                .classBuilder("Builder")
                .addGeneratedAnnotation(type)
                .apply {
                    addMembers()
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addMembers() {
        addField()
        addInit()
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
                .initializer(
                    "%T(null, null)",
                    type.draftClassName("$", "DraftImpl")
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addInit() {
        val props = type.properties.values.filter { isVisibilityControllable(it) }
        if (props.isEmpty()) {
            return
        }
        addInitializerBlock(
            CodeBlock
                .builder()
                .apply {
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