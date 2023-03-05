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
                    addIsVisibleFun(Int::class)
                    addIsVisibleFun(String::class)
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
        prop.valueFieldName?.let {
            addProperty(
                PropertySpec
                    .builder(
                        it,
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
        }
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
        prop.visibleFieldName?.let {
            addProperty(
                PropertySpec
                    .builder(it, BOOLEAN)
                    .addModifiers(KModifier.INTERNAL)
                    .initializer((!prop.isKotlinFormula && prop.idViewBaseProp == null).toString())
                    .mutable()
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.addProp(prop: ImmutableProp) {
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
                                    if (prop.idViewBaseProp !== null) {
                                        if (prop.isList) {
                                            addStatement(
                                                "return %N.map {it.%N}",
                                                prop.idViewBaseProp!!.name,
                                                prop.idViewBaseProp!!.targetType!!.idProp!!.name
                                            )
                                        } else {
                                            addStatement(
                                                "return %N%L%N",
                                                prop.idViewBaseProp!!.name,
                                                if (prop.isNullable) "?." else ".",
                                                prop.idViewBaseProp!!.targetType!!.idProp!!.name
                                            )
                                        }
                                    } else if (prop.isKotlinFormula) {
                                        addStatement("return super.%N", prop.name)
                                    }   else {
                                        if (prop.loadedFieldName === null) {
                                            addStatement("val %N = this.%N", prop.valueFieldName, prop.valueFieldName)
                                        }
                                        beginControlFlow(
                                            when {
                                                prop.loadedFieldName !== null -> "if (!${prop.loadedFieldName})"
                                                else -> "if (${prop.valueFieldName} === null)"
                                            }
                                        )
                                        addStatement(
                                            "throw %T(%T::class.java, %S)",
                                            UNLOADED_EXCEPTION_CLASS_NAME,
                                            prop.declaringType.className,
                                            prop.name
                                        )
                                        endControlFlow()
                                        addStatement("return %N", prop.valueFieldName)
                                    }
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
                                val baseProp = prop.idViewBaseProp
                                if (baseProp !== null) {
                                    if (prop.isList) {
                                        addStatement(
                                            "%L -> __isLoaded(%L) && %L.any { (it as %T).__isLoaded(%L) }",
                                            arg,
                                            baseProp.id,
                                            baseProp.name,
                                            IMMUTABLE_SPI_CLASS_NAME,
                                            baseProp.targetType!!.idProp!!.id
                                        )
                                    } else {
                                        addStatement(
                                            "%L -> __isLoaded(%L) && (%L as %T)%L__isLoaded(%L) ?: true",
                                            arg,
                                            baseProp.id,
                                            baseProp.name,
                                            IMMUTABLE_SPI_CLASS_NAME.copy(nullable = baseProp.isNullable),
                                            if (baseProp.isNullable) "?." else ".",
                                            baseProp.targetType!!.idProp!!.id
                                        )
                                    }
                                } else if (prop.isKotlinFormula) {
                                    add("%L ->", arg)
                                    indent()
                                    var first = true
                                    for (dependency in prop.dependencies) {
                                        if (first) {
                                            first = false
                                        } else {
                                            add(" && \n")
                                        }
                                        add("__isLoaded(%L)", dependency.id)
                                    }
                                    add("\n")
                                    unindent()
                                } else {
                                    val cond = prop.loadedFieldName ?: "${prop.valueFieldName} !== null"
                                    addStatement("%L -> %L", arg, cond)
                                }
                            }
                            addElseForNonExistingProp(argType)
                            endControlFlow()
                        }
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addIsVisibleFun(argType: KClass<*>) {
        addFunction(
            FunSpec
                .builder("__isVisible")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("prop", if (argType == Int::class) INT else STRING)
                .returns(BOOLEAN)
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            if (type.properties.values.any { it.visibleFieldName !== null}) {
                                add("return ")
                                beginControlFlow("when (prop)")
                                for (prop in type.propsOrderById) {
                                    if (prop.visibleFieldName !== null) {
                                        addStatement(
                                            "%L -> %L",
                                            if (argType == Int::class) prop.id else "\"${prop.name}\"",
                                            prop.visibleFieldName
                                        )
                                    }
                                }
                                addStatement("else -> true")
                                endControlFlow()
                            } else {
                                addStatement("return true")
                            }
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
                                if (prop.visibleFieldName !== null) {
                                    addStatement("hash = 31 * hash + %L.hashCode()", prop.visibleFieldName)
                                    if (prop.valueFieldName === null) {
                                        continue
                                    }
                                }
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
                                if (prop.visibleFieldName != null) {
                                    beginControlFlow(
                                        "if (%L != __other.__isVisible(%L))",
                                        prop.visibleFieldName,
                                        prop.id
                                    )
                                    addStatement("return false")
                                    endControlFlow()
                                    if (prop.valueFieldName == null) {
                                        continue
                                    }
                                }
                                val localLoadedName = "__${prop.name}Loaded"
                                val objLoadedName = prop.loadedFieldName ?: "${prop.valueFieldName} !== null"
                                add("val %L = \n", localLoadedName)
                                addStatement("    this.%L", objLoadedName)
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