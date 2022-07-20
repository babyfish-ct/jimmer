package org.babyfish.jimmer.ksp.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType
import kotlin.reflect.KClass

class DraftImplGenerator(
    private val type: ImmutableType,
    private val parent: TypeSpec.Builder
) {
    fun generate() {
        parent.addType(
            TypeSpec
                .classBuilder(DRAFT_IMPL)
                .superclass(type.draftClassName(PRODUCER, IMPLEMENTOR))
                .addSuperinterface(type.draftClassName)
                .addSuperinterface(DRAFT_SPI_CLASS_NAME)
                .addModifiers(KModifier.PRIVATE)
                .primaryConstructor(
                    FunSpec
                        .constructorBuilder()
                        .addParameter("ctx", DRAFT_CONTEXT_CLASS_NAME)
                        .addParameter("base", type.className.copy(nullable = true))
                        .build()
                )
                .apply {
                    addFields()
                    addIsLoadedProp(Int::class)
                    addIsLoadedProp(String::class)
                    addHashCodeFuns()
                    addEqualsFuns()
                    for (prop in type.properties.values) {
                        addProp(prop)
                        addPropFun(prop)
                    }
                    addUnloadFun(Int::class)
                    addUnloadFun(String::class)
                    addSetFun(Int::class)
                    addSetFun(String::class)
                    addDraftContextFun()
                    addResolveFun()
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addFields() {
        addProperty(
            PropertySpec
                .builder("__ctx", DRAFT_CONTEXT_CLASS_NAME)
                .addModifiers(KModifier.PRIVATE)
                .initializer("ctx")
                .build()
        )
        addProperty(
            PropertySpec
                .builder("__base", type.draftClassName(PRODUCER, IMPLEMENTOR))
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    "base as %T? ?: %T(null)",
                    type.draftClassName(PRODUCER, IMPLEMENTOR),
                    type.draftClassName(PRODUCER, IMPL)
                )
                .build()
        )
        addProperty(
            PropertySpec
                .builder(
                    "__modified",
                    type.draftClassName(PRODUCER, IMPL).copy(nullable = true)
                )
                .addModifiers(KModifier.PRIVATE)
                .mutable()
                .initializer("null")
                .build()
        )
        addProperty(
            PropertySpec
                .builder("__resolving", BOOLEAN)
                .mutable()
                .initializer("false")
                .build()
        )
    }

    private fun TypeSpec.Builder.addIsLoadedProp(argType: KClass<*>) {
        addFunction(
            FunSpec
                .builder("__isLoaded")
                .addParameter("prop", if (argType == Int::class) INT else STRING)
                .returns(BOOLEAN)
                .addModifiers(KModifier.OVERRIDE)
                .addCode("return %L.__isLoaded(prop)", CURRENT_IMPLEMENTOR)
                .build()
        )
    }

    private fun TypeSpec.Builder.addHashCodeFuns() {
        addFunction(
            FunSpec
                .builder("hashCode")
                .returns(INT)
                .addModifiers(KModifier.OVERRIDE)
                .addCode("return %T.identityHashCode(this)", SYSTEM_CLASS_NAME)
                .build()
        )
        addFunction(
            FunSpec
                .builder("__hashCode")
                .addParameter("shallow", BOOLEAN)
                .returns(INT)
                .addModifiers(KModifier.OVERRIDE)
                .addCode("return %T.identityHashCode(this)", SYSTEM_CLASS_NAME)
                .build()
        )
    }

    private fun TypeSpec.Builder.addEqualsFuns() {
        addFunction(
            FunSpec
                .builder("equals")
                .addParameter("other", ANY.copy(nullable = true))
                .returns(BOOLEAN)
                .addModifiers(KModifier.OVERRIDE)
                .addCode("return this === other")
                .build()
        )
        addFunction(
            FunSpec
                .builder("__equals")
                .addParameter("other", ANY.copy(nullable = true))
                .addParameter("shallow", BOOLEAN)
                .returns(BOOLEAN)
                .addModifiers(KModifier.OVERRIDE)
                .addCode("return this === other")
                .build()
        )
    }

    private fun TypeSpec.Builder.addProp(prop: ImmutableProp) {
        addProperty(
            PropertySpec
                .builder(prop.name, prop.typeName(), KModifier.OVERRIDE)
                .mutable()
                .getter(
                    FunSpec
                        .getterBuilder()
                        .apply {
                            if (prop.isList || prop.isScalarList) {
                                addCode(
                                    "return __ctx.toDraftList(%L.%L, %T::class.java, %L)",
                                    CURRENT_IMPLEMENTOR,
                                    prop.name,
                                    prop.targetTypeName(),
                                    prop.isAssociation
                                )
                            } else if (prop.isReference) {
                                addCode("return __ctx.toDraftObject(%L.%L)", CURRENT_IMPLEMENTOR, prop.name)
                            } else {
                                addCode("return %L.%L", CURRENT_IMPLEMENTOR, prop.name)
                            }
                        }
                        .build()
                )
                .setter(
                    FunSpec
                        .setterBuilder()
                        .addParameter(prop.name, prop.typeName())
                        .addCode(
                            CodeBlock
                                .builder()
                                .apply {
                                    addStatement("val __modified = %L", CURRENT_IMPL)
                                    if (prop.isList || prop.isScalarList) {
                                        addStatement(
                                            "__modified.%L = %T.of(__modified.%L, %L)",
                                            prop.valueFieldName,
                                            NON_SHARED_LIST_CLASS_NAME,
                                            prop.valueFieldName,
                                            prop.name
                                        )
                                    } else {
                                        addStatement(
                                            "__modified.%L = %L",
                                            prop.valueFieldName,
                                            prop.name
                                        )
                                    }
                                    prop.loadedFieldName?.let {
                                        addStatement("__modified.%L = true", it)
                                    }
                                }
                                .build()
                        )
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addPropFun(prop: ImmutableProp) {
        if (!prop.isAssociation && !prop.isList) {
            return
        }
        addFunction(
            FunSpec
                .builder(prop.name)
                .returns(prop.typeName(draft = true, overrideNullable = false))
                .addModifiers(KModifier.OVERRIDE)
                .apply {
                    if (prop.isList) {
                        addAnnotation(
                            AnnotationSpec
                                .builder(Suppress::class)
                                .apply {
                                    addMember("\"UNCHECKED_CAST\"")
                                }
                                .build()
                        )
                    }
                }
                .apply {
                    addCode(
                        CodeBlock
                            .builder()
                            .apply {
                                beginControlFlow(
                                    "if (!__isLoaded(%L) || %L === null)",
                                    prop.id,
                                    prop.name
                                )
                                if (prop.isList) {
                                    addStatement("%L = kotlin.collections.emptyList()", prop.name)
                                } else {
                                    addStatement(
                                        "%L = %T.produce {}",
                                        prop.name,
                                        prop.targetType!!.draftClassName(PRODUCER)
                                    )
                                }
                                endControlFlow()
                                addStatement(
                                    "return %L as %T",
                                    prop.name,
                                    prop.targetType!!.draftClassName.let {
                                        if (prop.isList) {
                                            MUTABLE_LIST.parameterizedBy(it)
                                        }
                                        else {
                                            it.copy(nullable = false)
                                        }
                                    }
                                )
                            }
                            .build()
                    )
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addUnloadFun(argType: KClass<*>) {
        addFunction(
            FunSpec
                .builder("__unload")
                .addParameter("prop", if (argType == Int::class) INT else STRING)
                .addModifiers(KModifier.OVERRIDE)
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            beginControlFlow("when (prop)")
                            for (prop in type.properties.values) {
                                if (argType == Int::class) {
                                    add(prop.id.toString())
                                } else {
                                    add("%S", prop.name)
                                }
                                add(" ->\n")
                                indent()
                                add("%L\n.", CURRENT_IMPL)
                                prop
                                    .loadedFieldName
                                    ?.let {
                                        add("%L = false", it)
                                    }
                                    ?: add("%L = null", prop.valueFieldName)
                                unindent()
                                add("\n")
                            }
                            addElseBranchForProp(argType)
                            endControlFlow()
                        }
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addSetFun(argType: KClass<*>) {
        addFunction(
            FunSpec
                .builder("__set")
                .addParameter("prop", if (argType == Int::class) INT else STRING)
                .addParameter("value", ANY.copy(nullable = true))
                .addModifiers(KModifier.OVERRIDE)
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            beginControlFlow("when (prop)")
                            for (prop in type.properties.values) {
                                if (argType == Int::class) {
                                    add(prop.id.toString())
                                } else {
                                    add("%S", prop.name)
                                }
                                add(" -> %L = value as %T", prop.name, prop.typeName())
                                add("\n")
                            }
                            addElseBranchForProp(argType)
                            endControlFlow()
                        }
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addDraftContextFun() {
        addFunction(
            FunSpec
                .builder("__draftContext")
                .returns(DRAFT_CONTEXT_CLASS_NAME)
                .addModifiers(KModifier.OVERRIDE)
                .addCode("return __ctx")
                .build()
        )
    }

    private fun TypeSpec.Builder.addResolveFun() {
        addFunction(
            FunSpec
                .builder("__resolve")
                .returns(ANY)
                .addModifiers(KModifier.OVERRIDE)
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            beginControlFlow("if (__resolving)")
                            addStatement("throw %T()", CIRCULAR_REFERENCE_EXCEPTION_CLASS_NAME)
                            endControlFlow()
                            addStatement("__resolving = true")
                            beginControlFlow("try")
                            addStatement("val base = __base")
                            addStatement("var modified = __modified")
                            beginControlFlow("if (modified === null)")
                            for (prop in type.properties.values) {
                                if (prop.isList || prop.isReference) {
                                    beginControlFlow("if (__isLoaded(%L))", prop.id)
                                    addStatement("val oldValue = base.%L", prop.name)
                                    addStatement(
                                        "val newValue = __ctx.%L(oldValue)",
                                        if (prop.isList || prop.isScalarList) {
                                            "resolveList"
                                        } else {
                                            "resolveObject"
                                        }
                                    )
                                    if (prop.isList) {
                                        add("if (oldValue !== newValue)")
                                    } else {
                                        add("if (!%T.equals(oldValue, newValue, true))", IMMUTABLE_SPI_CLASS_NAME)
                                    }
                                    beginControlFlow("")
                                    addStatement("%L = newValue", prop.name)
                                    endControlFlow()
                                    endControlFlow()
                                }
                            }
                            addStatement("modified = __modified")
                            nextControlFlow("else")
                            for (prop in type.properties.values) {
                                if (prop.isList || prop.isReference) {
                                    addStatement(
                                        "modified.%L = __ctx.%L(modified.%L)",
                                        prop.valueFieldName,
                                        if (prop.isList || prop.isScalarList) {
                                            "resolveList"
                                        } else {
                                            "resolveObject"
                                        },
                                        prop.valueFieldName
                                    )
                                }
                            }
                            endControlFlow()
                            beginControlFlow(
                                "if (modified === null || %T.equals(base, modified, true))",
                                IMMUTABLE_SPI_CLASS_NAME
                            )
                            addStatement("return base")
                            endControlFlow()
                            addStatement("return modified")
                            nextControlFlow("finally")
                            addStatement("__resolving = false")
                            endControlFlow()
                        }
                        .build()
                )
                .build()
        )
    }
}