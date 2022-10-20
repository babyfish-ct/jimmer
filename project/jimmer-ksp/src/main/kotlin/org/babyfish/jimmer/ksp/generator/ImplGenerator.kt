package org.babyfish.jimmer.ksp.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType
import kotlin.reflect.KClass

class ImplGenerator(
    private val type: ImmutableType,
    private val parent: TypeSpec.Builder
) {
    fun generate() {
        parent.addType(
            TypeSpec
                .classBuilder(IMPL)
                .addModifiers(KModifier.PRIVATE)
                .superclass(type.draftClassName(PRODUCER, IMPLEMENTOR))
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter(
                            "base",
                            type.className.copy(nullable = true)
                        )
                        .build()
                )
                .apply {
                    for (prop in type.properties.values) {
                        addFields(prop)
                    }
                    addInitializer()
                    for (prop in type.properties.values) {
                        addProp(prop)
                    }
                    addIsLoadedFun(Int::class)
                    addIsLoadedFun(String::class)
                    addHashCodeFun(true)
                    addHashCodeFun(false)
                    addParameterizedHashCode()
                    addEqualsFun(true)
                    addEqualsFun(false)
                    addParameterizedEquals()
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addFields(prop: ImmutableProp) {
        addProperty(
            PropertySpec
                .builder(
                    prop.valueFieldName,
                    if (prop.isList) {
                        NON_SHARED_LIST_CLASS_NAME
                            .parameterizedBy(prop.targetTypeName())
                            .copy(nullable = true)
                    } else {
                        prop.typeName().copy(nullable = !prop.isPrimitive)
                    }
                )
                .addModifiers(KModifier.INTERNAL)
                .mutable()
                .build()
        )
        prop.loadedFieldName?.let {
            addProperty(
                PropertySpec
                    .builder(it, BOOLEAN)
                    .addModifiers(KModifier.INTERNAL)
                    .mutable()
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.addInitializer() {

        addInitializerBlock(
            CodeBlock
                .builder().apply {
                    addStatement("val from = base as %T?", type.draftClassName(PRODUCER, IMPLEMENTOR))
                    for (prop in type.properties.values) {
                        beginControlFlow("if (from !== null && from.__isLoaded(%L))", prop.id)
                        if (prop.isList) {
                            addStatement(
                                "this.%L = %T.of(null, from.%L)",
                                prop.valueFieldName,
                                NON_SHARED_LIST_CLASS_NAME,
                                prop.name
                            )
                        } else {
                            addStatement("this.%L = from.%L", prop.valueFieldName, prop.name)
                        }
                        prop.loadedFieldName?.let {
                            addStatement("this.%L = true", it)
                        }
                        nextControlFlow("else")
                        addStatement(
                            "this.%L = %L",
                            prop.valueFieldName,
                            if (prop.isPrimitive) {
                                when (prop.typeName()) {
                                    BOOLEAN -> "false"
                                    CHAR -> "0.toChar()"
                                    FLOAT -> "0F"
                                    DOUBLE -> "0.0"
                                    else -> "0"
                                }
                            } else {
                                "null"
                            }
                        )
                        prop.loadedFieldName?.let {
                            addStatement("this.%L = false", it)
                        }
                        endControlFlow()
                    }
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addProp(prop: ImmutableProp) {
        val ifUnLoaded = prop
            .loadedFieldName
            ?.let {
                "if (!$it)"
            }
            ?: "if (${prop.valueFieldName} === null)"
        addProperty(
            PropertySpec
                .builder(prop.name, prop.typeName())
                .addModifiers(KModifier.OVERRIDE)
                .getter(
                    FunSpec
                        .getterBuilder()
                        .addCode(
                            CodeBlock
                                .builder()
                                .apply {
                                    addStatement("val %L = this.%L", prop.valueFieldName, prop.valueFieldName)
                                    beginControlFlow(ifUnLoaded)
                                    addStatement(
                                        "throw %T(%T::class.java, %S)",
                                        UNLOADED_EXCEPTION_CLASS_NAME,
                                        prop.declaringType.className,
                                        prop.name
                                    )
                                    endControlFlow()
                                    addStatement("return %L", prop.valueFieldName)
                                }
                                .build()
                        )
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addIsLoadedFun(argType: KClass<*>) {
        addFunction(
            FunSpec
                .builder("__isLoaded")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("prop", if (argType == Int::class) INT else STRING)
                .returns(BOOLEAN)
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            add("return ")
                            beginControlFlow("when (prop)")
                            for (prop in type.propsOrderById) {
                                val arg = if (argType == Int::class) prop.id else "\"${prop.name}\""
                                val cond = prop.loadedFieldName ?: "${prop.valueFieldName} !== null"
                                addStatement("%L -> %L", arg, cond)
                            }
                            addElseBranchForProp(argType)
                            endControlFlow()
                        }
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addHashCodeFun(shallow: Boolean) {
        addFunction(
            FunSpec
                .builder(if (shallow) "__shallowHashCode" else "hashCode")
                .apply {
                    if (!shallow) {
                        addModifiers(KModifier.OVERRIDE)
                    }
                }
                .returns(INT)
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            addStatement("var hash = 1")
                            for (prop in type.properties.values) {
                                beginControlFlow(
                                    "if (%L)",
                                    prop.loadedFieldName ?: "${prop.valueFieldName} !== null"
                                )
                                add("hash = 31 * hash + ")
                                if (shallow && prop.isAssociation) {
                                    add("%T.identityHashCode(%L)", SYSTEM_CLASS_NAME, prop.valueFieldName)
                                } else {
                                    if (prop.isNullable) {
                                        add("(%L?.hashCode() ?: 0)", prop.valueFieldName)
                                    } else {
                                        add("%L.hashCode()", prop.valueFieldName)
                                    }
                                }
                                add("\n")
                                if (!shallow && prop.isId) {
                                    addStatement("return hash")
                                }
                                endControlFlow()
                            }
                            addStatement("return hash")
                        }
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addParameterizedHashCode() {
        addFunction(
            FunSpec
                .builder("__hashCode")
                .addParameter("shallow", BOOLEAN)
                .returns(INT)
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("return if (shallow) __shallowHashCode() else hashCode()")
                .build()
        )
    }

    private fun TypeSpec.Builder.addEqualsFun(shallow: Boolean) {
        addFunction(
            FunSpec
                .builder(if (shallow) "__shallowEquals" else "equals")
                .addParameter("other", ANY.copy(nullable = true))
                .apply {
                    if (!shallow) {
                        addModifiers(KModifier.OVERRIDE)
                    }
                }
                .returns(BOOLEAN)
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            beginControlFlow("if (other === null || this::class != other::class)")
                            addStatement("return false")
                            endControlFlow()
                            addStatement("val otherImpl = other as %T", type.draftClassName(PRODUCER, IMPL))
                            for (prop in type.properties.values) {
                                val localLoadedName = "__${prop.name}Loaded"
                                val objLoadedName = prop.loadedFieldName ?: "${prop.valueFieldName} !== null"
                                addStatement("val %L = this.%L", localLoadedName, objLoadedName)
                                beginControlFlow(
                                    "if (%L != (otherImpl.%L))",
                                    localLoadedName,
                                    prop.loadedFieldName ?: "${prop.valueFieldName} !== null"
                                )
                                addStatement("return false")
                                endControlFlow()
                                if (prop.isId) {
                                    beginControlFlow("if (%L)", localLoadedName)
                                    addStatement(
                                        "return this.%L %L otherImpl.%L",
                                        prop.valueFieldName,
                                        if (shallow && prop.isAssociation) "!==" else "!=",
                                        prop.name
                                    )
                                    endControlFlow()
                                } else {
                                    beginControlFlow(
                                        "if (%L && this.%L %L otherImpl.%L)",
                                        localLoadedName,
                                        prop.valueFieldName,
                                        if (shallow && prop.isAssociation) "!==" else "!=",
                                        prop.name
                                    )
                                    addStatement("return false")
                                    endControlFlow()
                                }
                            }
                            addStatement("return true")
                        }
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addParameterizedEquals() {
        addFunction(
            FunSpec
                .builder("__equals")
                .addParameter("obj", ANY.copy(nullable = true))
                .addParameter("shallow", BOOLEAN)
                .returns(BOOLEAN)
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("return if (shallow) __shallowEquals(obj) else equals(obj)")
                .build()
        )
    }
}