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
                .addSuperinterface(CLONEABLE_CLASS_NAME)
                .apply {
                    for (prop in type.properties.values) {
                        addFields(prop)
                    }
                    for (prop in type.properties.values) {
                        addProp(prop)
                    }
                    addCloneFun()
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
                .apply {
                    val defaultValue = if (prop.isPrimitive) {
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
                    initializer(defaultValue)
                }
                .mutable()
                .build()
        )
        prop.loadedFieldName?.let {
            addProperty(
                PropertySpec
                    .builder(it, BOOLEAN)
                    .addModifiers(KModifier.INTERNAL)
                    .initializer("false")
                    .mutable()
                    .build()
            )
        }
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
                        .addAnnotation(JSON_IGNORE_CLASS_NAME)
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

    private fun TypeSpec.Builder.addCloneFun() {
        addFunction(
            FunSpec
                .builder("clone")
                .addModifiers(KModifier.OVERRIDE)
                .returns(type.draftClassName(PRODUCER, IMPL))
                .addStatement("return super.clone() as %T", type.draftClassName(PRODUCER, IMPL))
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
                                if (shallow && prop.isAssociation(false)) {
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
                            addStatement("val __other = other as? %T", type.draftClassName(PRODUCER, IMPLEMENTOR))
                            beginControlFlow("if (__other === null)")
                            addStatement("return false")
                            endControlFlow()
                            for (prop in type.properties.values) {
                                val localLoadedName = "__${prop.name}Loaded"
                                val objLoadedName = prop.loadedFieldName ?: "${prop.valueFieldName} !== null"
                                addStatement("val %L = this.%L", localLoadedName, objLoadedName)
                                beginControlFlow(
                                    "if (%L != (__other.__isLoaded(%L)))",
                                    localLoadedName,
                                    prop.id
                                )
                                addStatement("return false")
                                endControlFlow()
                                if (prop.isId && !shallow) {
                                    beginControlFlow("if (%L)", localLoadedName)
                                    addStatement(
                                        "return this.%L == __other.%L",
                                        prop.valueFieldName,
                                        prop.name
                                    )
                                    endControlFlow()
                                } else {
                                    beginControlFlow(
                                        "if (%L && this.%L %L __other.%L)",
                                        localLoadedName,
                                        prop.valueFieldName,
                                        if (shallow && prop.isAssociation(false)) "!==" else "!=",
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