package org.babyfish.jimmer.ksp.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType

class FetcherDslGenerator(
    private val type: ImmutableType,
    private val parent: FileSpec.Builder
) {

    fun generate() {
        parent.addType(
            TypeSpec
                .classBuilder("${type.simpleName}$FETCHER_DSL")
                .addAnnotation(DSL_SCOPE_CLASS_NAME)
                .apply {
                    addField()
                    addConstructor()
                    addInternallyGetFetcher()
                    addDeleteFun("allScalarFields")
                    addDeleteFun("allTableFields")
                    for (prop in type.properties.values) {
                        if (!prop.isId) {
                            addSimpleProp(prop)
                            addAssociationProp(prop, false)
                            addAssociationProp(prop, true)
                        }
                    }
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addField() {
        addProperty(
            PropertySpec
                .builder(
                    "_fetcher",
                    FETCHER_CLASS_NAME.parameterizedBy(
                        type.className
                    ),
                    KModifier.PRIVATE
                )
                .mutable()
                .initializer("fetcher")
                .build()
        )
    }

    private fun TypeSpec.Builder.addInternallyGetFetcher() {
        addFunction(
            FunSpec
                .builder("internallyGetFetcher")
                .returns(
                    FETCHER_CLASS_NAME.parameterizedBy(
                        type.className
                    )
                )
                .addStatement("return _fetcher")
                .build()
        )
    }

    private fun TypeSpec.Builder.addConstructor() {
        primaryConstructor(
            FunSpec
                .constructorBuilder()
                .addParameter(
                    "fetcher",
                    FETCHER_CLASS_NAME.parameterizedBy(
                        type.className
                    )
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addDeleteFun(funName: String) {
        addFunction(
            FunSpec
                .builder(funName)
                .addCode("_fetcher = _fetcher.%L()", funName)
                .build()
        )
    }

    private fun TypeSpec.Builder.addSimpleProp(prop: ImmutableProp) {
        addFunction(
            FunSpec
                .builder(prop.name)
                .addParameter(
                    ParameterSpec
                        .builder("enabled", BOOLEAN)
                        .defaultValue("true")
                        .build()
                )
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            add("_fetcher = ")
                            beginControlFlow("if (enabled)")
                            addStatement("_fetcher.add(%S)", prop.name)
                            nextControlFlow("else")
                            addStatement("_fetcher.remove(%S)", prop.name)
                            endControlFlow()
                        }
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addAssociationProp(prop: ImmutableProp, lambda: Boolean) {
        if (!prop.isAssociation(true) || !prop.targetType!!.isEntity) {
            return
        }
        val (cfgDslClassName, cfgTranName) =
            if (prop.targetType === prop.declaringType) {
                if (prop.isList) {
                    K_RECURSIVE_LIST_FIELD_DSL to "recursiveList"
                } else {
                    K_RECURSIVE_FIELD_DSL to "recursive"
                }
            } else {
                if (prop.isList) {
                    K_LIST_FIELD_DSL to "list"
                } else {
                    K_FIELD_DSL to "simple"
                }
            }
        val cfgBlockParameter = ParameterSpec
            .builder(
                "cfgBlock",
                LambdaTypeName.get(
                    cfgDslClassName
                        .parameterizedBy(
                            prop.targetTypeName(overrideNullable = false)
                        ),
                    emptyList(),
                    UNIT
                ).copy(nullable = true)
            )
            .defaultValue("null")
            .build()
        addFunction(
            FunSpec
                .builder(prop.name)
                .apply {
                    if (lambda) {
                        addParameter(cfgBlockParameter)
                        addParameter(
                            "childBlock",
                            LambdaTypeName.get(
                                prop.targetType!!.fetcherDslClassName,
                                emptyList(),
                                UNIT
                            )
                        )
                    } else {
                        addParameter(
                            "childFetcher",
                            FETCHER_CLASS_NAME.parameterizedBy(
                                prop.targetTypeName()
                            )
                        )
                        addParameter(cfgBlockParameter)
                    }
                }
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            add("_fetcher = _fetcher.add(\n")
                            indent()
                            add("%S,\n", prop.name)
                            if (lambda) {
                                add(
                                    "%T(%T::class).by(childBlock),\n",
                                    NEW_FETCHER_FUN_CLASS_NAME,
                                    prop.targetTypeName(overrideNullable = false)
                                )
                            } else {
                                add("childFetcher,\n")
                            }
                            add("%T.%L(cfgBlock)\n", JAVA_FIELD_CONFIG_UTILS, cfgTranName)
                            unindent()
                            addStatement(")")
                        }
                        .build()
                )
                .build()
        )
    }
}