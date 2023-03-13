package org.babyfish.jimmer.ksp.generator

import com.google.devtools.ksp.symbol.KSAnnotation
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.get
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType
import javax.validation.constraints.Email
import javax.validation.constraints.Pattern
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
                    addIsVisibleProp(Int::class)
                    addIsVisibleProp(String::class)
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
                    addShowFun(Int::class)
                    addShowFun(String::class)
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
                .builder("__base", type.draftClassName(PRODUCER, IMPL))
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    "base as %T? ?: %T()",
                    type.draftClassName(PRODUCER, IMPL),
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
        addCompanionObject()
    }

    private fun TypeSpec.Builder.addIsLoadedProp(argType: KClass<*>) {
        addFunction(
            FunSpec
                .builder("__isLoaded")
                .addParameter("prop", if (argType == Int::class) INT else STRING)
                .returns(BOOLEAN)
                .addModifiers(KModifier.OVERRIDE)
                .addCode("return %L.__isLoaded(prop)", UNMODIFIED)
                .build()
        )
    }

    private fun TypeSpec.Builder.addIsVisibleProp(argType: KClass<*>) {
        addFunction(
            FunSpec
                .builder("__isVisible")
                .addParameter("prop", if (argType == Int::class) INT else STRING)
                .returns(BOOLEAN)
                .addModifiers(KModifier.OVERRIDE)
                .addCode("return %L.__isVisible(prop)", UNMODIFIED)
                .build()
        )
    }

    private fun TypeSpec.Builder.addHashCodeFuns() {
        addFunction(
            FunSpec
                .builder("hashCode")
                .returns(INT)
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("return %L.hashCode()", UNMODIFIED)
                .build()
        )
        addFunction(
            FunSpec
                .builder("__hashCode")
                .addParameter("shallow", BOOLEAN)
                .returns(INT)
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("return %L.__hashCode(shallow)", UNMODIFIED)
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
                .addStatement("return %L.equals(other)", UNMODIFIED)
                .build()
        )
        addFunction(
            FunSpec
                .builder("__equals")
                .addParameter("other", ANY.copy(nullable = true))
                .addParameter("shallow", BOOLEAN)
                .returns(BOOLEAN)
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("return %L.__equals(other, shallow)", UNMODIFIED)
                .build()
        )
    }

    private fun TypeSpec.Builder.addProp(prop: ImmutableProp) {
        addProperty(
            PropertySpec
                .builder(prop.name, prop.typeName(), KModifier.OVERRIDE)
                .mutable(!prop.isKotlinFormula)
                .getter(
                    FunSpec
                        .getterBuilder()
                        .addAnnotation(JSON_IGNORE_CLASS_NAME)
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
                            } else if (prop.isList || prop.isScalarList) {
                                addCode(
                                    "return __ctx.toDraftList(%L.%L, %T::class.java, %L)",
                                    UNMODIFIED,
                                    prop.name,
                                    prop.targetTypeName(),
                                    prop.isAssociation(false)
                                )
                            } else if (prop.isReference) {
                                addCode("return __ctx.toDraftObject(%L.%L)", UNMODIFIED, prop.name)
                            } else {
                                addCode("return %L.%L", UNMODIFIED, prop.name)
                            }
                        }
                        .build()
                )
                .apply {
                    if (!prop.isKotlinFormula) {
                        setter(
                            FunSpec
                                .setterBuilder()
                                .addParameter(prop.name, prop.typeName())
                                .addCode(
                                    CodeBlock
                                        .builder()
                                        .apply {
                                            val idViewBaseProp = prop.idViewBaseProp
                                            if (idViewBaseProp !== null) {
                                                addStatement(
                                                    "%N = %L%L%N { %M(it) }",
                                                    idViewBaseProp.name,
                                                    prop.name,
                                                    if (idViewBaseProp.isNullable) "?." else ".",
                                                    if (idViewBaseProp.isList) "map" else "let",
                                                    MAKE_ID_ONLY
                                                )
                                            } else {
                                                ValidationGenerator(prop, this).generate()
                                                addStatement("val __tmpModified = %L", MODIFIED)
                                                if (prop.isList || prop.isScalarList) {
                                                    addStatement(
                                                        "__tmpModified.%L = %T.of(__tmpModified.%L, %L)",
                                                        prop.valueFieldName,
                                                        NON_SHARED_LIST_CLASS_NAME,
                                                        prop.valueFieldName,
                                                        prop.name
                                                    )
                                                } else {
                                                    addStatement(
                                                        "__tmpModified.%L = %L",
                                                        prop.valueFieldName,
                                                        prop.name
                                                    )
                                                }
                                                prop.loadedFieldName?.let {
                                                    addStatement("__tmpModified.%L = true", it)
                                                }
                                            }
                                        }
                                        .build()
                                )
                                .build()
                        )
                    }
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addPropFun(prop: ImmutableProp) {
        if (!prop.isAssociation(false) && !prop.isList) {
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
                                if (prop.isNullable) {
                                    beginControlFlow(
                                        "if (!__isLoaded(%L) || %L === null)",
                                        prop.id,
                                        prop.name
                                    )
                                } else {
                                    beginControlFlow(
                                        "if (!__isLoaded(%L))",
                                        prop.id
                                    )
                                }
                                if (prop.isList) {
                                    addStatement("%L = emptyList()", prop.name)
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
                                    prop.typeName(true, overrideNullable = false)
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
                            for (prop in type.propsOrderById) {
                                if (argType == Int::class) {
                                    add(prop.id.toString())
                                } else {
                                    add("%S", prop.name)
                                }
                                add(" ->")
                                indent()
                                when {
                                    prop.idViewBaseProp !== null ->
                                        addStatement("__unload(%L)", prop.idViewBaseProp!!.id)
                                    prop.isKotlinFormula ->
                                        addStatement("{}")
                                    prop.loadedFieldName !== null ->
                                        add("%L\n.%L = false", MODIFIED, prop.loadedFieldName)
                                    else -> add("%L\n.%L = null", MODIFIED, prop.valueFieldName)
                                }
                                unindent()
                                add("\n")
                            }
                            addElseForNonExistingProp(type, argType)
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
                            for (prop in type.propsOrderById) {
                                if (prop.isKotlinFormula) {
                                    continue
                                }
                                if (argType == Int::class) {
                                    add(prop.id.toString())
                                } else {
                                    add("%S", prop.name)
                                }
                                add(" -> this.%L = value as %T?", prop.name, prop.typeName(overrideNullable = false))
                                if (!prop.isNullable) {
                                    add("\n    ?: throw IllegalArgumentException(%S)", "'${prop.name} cannot be null")
                                }
                                add("\n")
                            }
                            addElseForNonExistingProp(type, argType)
                            endControlFlow()
                        }
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addShowFun(argType: KClass<*>) {
        addFunction(
            FunSpec
                .builder("__show")
                .addParameter("prop", if (argType == Int::class) INT else STRING)
                .addParameter("visible", BOOLEAN)
                .addModifiers(KModifier.OVERRIDE)
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            if (type.properties.values.any { it.visibleFieldName !== null }) {
                                beginControlFlow("when (prop)")
                                for (prop in type.propsOrderById) {
                                    if (prop.visibleFieldName != null) {
                                        if (argType == Int::class) {
                                            add(prop.id.toString())
                                        } else {
                                            add("%S", prop.name)
                                        }
                                        add(" -> %L\n.%L = visible\n", MODIFIED, prop.visibleFieldName)
                                    }
                                }
                                add("else -> throw IllegalArgumentException(\n")
                                indent()
                                add(
                                    "%S + \nprop + \n%S",
                                    "Illegal property " +
                                        (if (argType == String::class) "name" else "id") +
                                        ": \"",
                                    "\",it does not exists or is its visibility is not controllable)"
                                )
                                unindent()
                                add("\n)")
                                endControlFlow()
                            } else {
                                add(
                                    "%S + \nprop + \n%S",
                                    "Illegal property " +
                                        (if (argType == String::class) "name" else "id") +
                                        ": \"",
                                    "\",it does not exists or is its visibility is not controllable)"
                                )
                            }
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
                            addStatement("var __tmpModified = __modified")
                            if (type.properties.values.any { it.isList || it.isReference }) {
                                beginControlFlow("if (__tmpModified === null)")
                                for (prop in type.properties.values) {
                                    if (prop.idViewBaseProp == null && (prop.isList || prop.isReference)) {
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
                                addStatement("__tmpModified = __modified")
                                nextControlFlow("else")
                                for (prop in type.properties.values) {
                                    if (prop.idViewBaseProp !== null) {
                                        continue
                                    } else if (prop.isList) {
                                        addStatement(
                                            "__tmpModified.%L = %T.of(__tmpModified.%L, __ctx.%L(__tmpModified.%L))",
                                            prop.valueFieldName,
                                            NON_SHARED_LIST_CLASS_NAME,
                                            prop.valueFieldName,
                                            "resolveList",
                                            prop.valueFieldName
                                        )
                                    } else if (prop.isReference) {
                                        addStatement(
                                            "__tmpModified.%L = __ctx.%L(__tmpModified.%L)",
                                            prop.valueFieldName,
                                            "resolveObject",
                                            prop.valueFieldName
                                        )
                                    }
                                }
                                endControlFlow()
                            }
                            beginControlFlow(
                                "if (__tmpModified === null || %T.equals(base, __tmpModified, true))",
                                IMMUTABLE_SPI_CLASS_NAME
                            )
                            addStatement("return base")
                            endControlFlow()
                            for ((className, _) in type.validationMessages) {
                                addStatement("%L.validate(__tmpModified)", validatorFieldName(className))
                            }
                            addStatement("return __tmpModified")
                            nextControlFlow("finally")
                            addStatement("__resolving = false")
                            endControlFlow()
                        }
                        .build()
                )
                .build()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun TypeSpec.Builder.addCompanionObject() {
        val emailPropMap = type.properties.values
            .associateBy({it}) {
                it.annotation(Email::class)
            }
            .filterValues { it !== null } as Map<ImmutableProp, KSAnnotation>
        val patternPropMultiMap = type.properties.values
            .associateBy({it}) {
                it.annotations(Pattern::class)
            }
            .filterValues { it.isNotEmpty() }
        if (emailPropMap.isNotEmpty() ||
            patternPropMultiMap.isNotEmpty() ||
            type.validationMessages.isNotEmpty() ||
            type.properties.values.any { it.validationMessages.isNotEmpty() }
        ) {
            addType(
                TypeSpec
                    .companionObjectBuilder()
                    .apply {
                        if (emailPropMap.isNotEmpty()) {
                            addProperty(
                                PropertySpec
                                    .builder(DRAFT_FIELD_EMAIL_PATTERN, PATTERN_CLASS_NAME, KModifier.PRIVATE)
                                    .initializer("%T.compile(%S)", PATTERN_CLASS_NAME, EMAIL_PATTERN)
                                    .build()
                            )
                        }
                        for ((prop, patterns) in patternPropMultiMap) {
                            for (i in patterns.indices) {
                                addProperty(
                                    PropertySpec
                                        .builder(regexpPatternFieldName(prop, i), PATTERN_CLASS_NAME, KModifier.PRIVATE)
                                        .initializer("%T.compile(%S)", PATTERN_CLASS_NAME, patterns[i].get<String>("regexp"))
                                        .build()
                                )
                            }
                        }
                        for ((className, message) in type.validationMessages) {
                            addProperty(
                                PropertySpec
                                    .builder(
                                        validatorFieldName(className),
                                        VALIDATOR_CLASS_NAME.parameterizedBy(
                                            type.className
                                        ),
                                        KModifier.PRIVATE
                                    )
                                    .initializer(
                                        "%T(%T::class.java, %S, %T::class.java, null)",
                                        VALIDATOR_CLASS_NAME,
                                        className,
                                        message,
                                        type.className
                                    )
                                    .build()
                            )
                        }
                        for (prop in type.properties.values) {
                            for ((className, message) in prop.validationMessages) {
                                addProperty(
                                    PropertySpec
                                        .builder(
                                            validatorFieldName(prop, className),
                                            VALIDATOR_CLASS_NAME.parameterizedBy(
                                                prop.typeName()
                                            ),
                                            KModifier.PRIVATE
                                        )
                                        .initializer(
                                            "%T(%T::class.java, %S, %T::class.java, %L)",
                                            VALIDATOR_CLASS_NAME,
                                            className,
                                            message,
                                            type.className,
                                            prop.id.toString()
                                        )
                                        .build()
                                )
                            }
                        }
                    }
                    .build()
            )
        }
    }

    companion object {

        @JvmStatic
        private val MAKE_ID_ONLY =
            MemberName("org.babyfish.jimmer.kt", "makeIdOnly")
    }
}