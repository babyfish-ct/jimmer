package org.babyfish.jimmer.ksp.immutable.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType
import org.babyfish.jimmer.sql.JoinTable
import org.babyfish.jimmer.sql.ManyToOne

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
                            addAssociationProp(prop, false, false)
                            addAssociationProp(prop, false, true)
                            addAssociationProp(prop, true, false)
                            addAssociationProp(prop, true, true)
                            addRecursiveProp(prop)
                            addSimplePropWithFetchType(prop)
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

    private fun TypeSpec.Builder.addSimplePropWithFetchType(prop: ImmutableProp) {
        val associationProp = prop.idViewBaseProp ?: prop
        if (associationProp.isTransient || !associationProp.isAssociation(true)) {
            return
        }
        if (prop.isReverse && associationProp.annotation(ManyToOne::class) === null && associationProp.annotation(JoinTable::class) === null) {
            return
        }
        addFunction(
            FunSpec
                .builder(prop.name)
                .addParameter(
                    ParameterSpec
                        .builder("idOnlyFetchType", ID_ONLY_FETCH_TYPE)
                        .build()
                )
                .addCode(
                    CodeBlock
                        .builder()
                        .add("_fetcher = _fetcher.add(%S, idOnlyFetchType)", prop.name)
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addAssociationProp(
        prop: ImmutableProp,
        enabled: Boolean,
        lambda: Boolean
    ) {
        if (!prop.isAssociation(true) || !prop.targetType!!.isEntity) {
            return
        }
        val (cfgDslClassName, cfgTranName) =
            if (prop.isList) {
                K_LIST_FIELD_DSL to "list"
            } else {
                K_FIELD_DSL to "simple"
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
                    if (enabled) {
                        addParameter(
                            ParameterSpec
                                .builder("enabled", BOOLEAN)
                                .build()
                        )
                    }
                    if (lambda) {
                        if (!prop.isRemote) {
                            addParameter(cfgBlockParameter)
                        }
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
                                prop.targetTypeName(overrideNullable = false)
                            )
                        )
                        if (!prop.isRemote) {
                            addParameter(cfgBlockParameter)
                        }
                    }
                }
                .apply {
                    if (enabled) {
                        addCode(
                            CodeBlock
                                .builder()
                                .apply {
                                    beginControlFlow("if (!enabled)")
                                    addStatement("_fetcher = _fetcher.remove(%S)", prop.name)
                                    nextControlFlow("else")
                                    add("%N(", prop.name)
                                    if (lambda) {
                                        if (!prop.isRemote) {
                                            add("cfgBlock, ")
                                        }
                                        add("childBlock)\n")
                                    } else {
                                        add("childFetcher")
                                        if (!prop.isRemote) {
                                            add(", cfgBlock")
                                        }
                                        add(")\n")
                                    }
                                    endControlFlow()
                                }
                                .build()
                        )
                    } else {
                        addCode(
                            CodeBlock
                                .builder()
                                .apply {
                                    add("_fetcher = _fetcher.add(\n")
                                    indent()
                                    add("%S,\n", prop.name)
                                    if (lambda) {
                                        add(
                                            "%T(%T::class).by(childBlock)",
                                            NEW_FETCHER_FUN_CLASS_NAME,
                                            prop.targetTypeName(overrideNullable = false)
                                        )
                                    } else {
                                        add("childFetcher")
                                    }
                                    if (!prop.isRemote) {
                                        add(",\n%T.%L(cfgBlock)", JAVA_FIELD_CONFIG_UTILS, cfgTranName)
                                    }
                                    unindent()
                                    add("\n)\n")
                                }
                                .build()
                        )
                    }
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addRecursiveProp(prop: ImmutableProp) {
        if (!prop.isRecursive) {
            return
        }
        val (cfgDslClassName, cfgTranName) =
            if (prop.isList) {
                K_RECURSIVE_LIST_FIELD_DSL to "recursiveList"
            } else {
                K_RECURSIVE_FIELD_DSL to "recursive"
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
                .builder(prop.name + '*')
                .addParameter(cfgBlockParameter)
                .apply {
                    addCode(
                        CodeBlock
                            .builder()
                            .apply {
                                add("_fetcher = _fetcher.addRecursion(\n")
                                indent()
                                add("%S,\n", prop.name)
                                add("%T.%N(cfgBlock)\n", JAVA_FIELD_CONFIG_UTILS, cfgTranName)
                                unindent()
                                add(")\n")
                            }
                            .build()
                    )
                }
                .build()
        )
    }
}