package org.babyfish.jimmer.ksp.generator

import com.google.devtools.ksp.symbol.KSAnnotation
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.get
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType
import org.babyfish.jimmer.meta.PropId
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
                .addSuperinterface(type.draftClassName(PRODUCER, IMPLEMENTOR))
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
                    addIsLoadedProp(PropId::class)
                    addIsLoadedProp(String::class)
                    addIsVisibleProp(PropId::class)
                    addIsVisibleProp(String::class)
                    addHashCodeFuns()
                    addEqualsFuns()
                    addToStringFun()
                    for (prop in type.properties.values) {
                        addProp(prop)
                        addPropFun(prop)
                    }
                    addUnloadFun(PropId::class)
                    addUnloadFun(String::class)
                    addSetFun(PropId::class)
                    addSetFun(String::class)
                    addShowFun(PropId::class)
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
                .builder(
                    "__base",
                    type.draftClassName(PRODUCER, IMPL).copy(nullable = true)
                )
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    "base as %T?",
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
                .initializer(
                    "if (base === null) Impl() else null"
                )
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
                .addParameter("prop", argType)
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
                .addParameter("prop", argType)
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

    private fun TypeSpec.Builder.addToStringFun() {
        addFunction(
            FunSpec
                .builder("toString")
                .addModifiers(KModifier.OVERRIDE)
                .returns(STRING)
                .addCode("return %T.toString(%L)", IMMUTABLE_OBJECTS_CLASS_NAME, UNMODIFIED)
                .build()
        )
    }

    private fun TypeSpec.Builder.addProp(prop: ImmutableProp) {
        val mutable = prop.manyToManyViewBaseProp === null && !prop.isKotlinFormula
        addProperty(
            PropertySpec
                .builder(prop.name, prop.typeName(), KModifier.OVERRIDE)
                .mutable(mutable)
                .getter(
                    FunSpec
                        .getterBuilder()
                        .addAnnotation(JSON_IGNORE_CLASS_NAME)
                        .apply {
                            val idViewBaseProp = prop.idViewBaseProp
                            when {
                                idViewBaseProp !== null && prop.isList ->
                                    addCode(
                                        "return %T(%T.type, %L)",
                                        MUTABLE_ID_VIEW_CLASS_NAME,
                                        idViewBaseProp.targetType!!.draftClassName("$"),
                                        idViewBaseProp.name
                                    )
                                prop.isList || prop.isScalarList ->
                                    addCode(
                                        "return __ctx.toDraftList(%L.%L, %T::class.java, %L)",
                                        UNMODIFIED,
                                        prop.name,
                                        prop.targetTypeName(),
                                        prop.isAssociation(false)
                                    )
                                prop.isReference ->
                                    addCode("return __ctx.toDraftObject(%L.%L)", UNMODIFIED, prop.name)
                                else ->
                                    addCode("return %L.%L", UNMODIFIED, prop.name)
                            }
                        }
                        .build()
                )
                .apply {
                    if (mutable) {
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
                                                if (idViewBaseProp.isList || idViewBaseProp.isNullable) {
                                                    addStatement(
                                                        "%N = %L%L%N { %M(it) }",
                                                        idViewBaseProp.name,
                                                        prop.name,
                                                        if (idViewBaseProp.isNullable) "?." else ".",
                                                        if (idViewBaseProp.isList) "map" else "let",
                                                        MAKE_ID_ONLY
                                                    )
                                                } else {
                                                    addStatement(
                                                        "%N = %M(%L)",
                                                        idViewBaseProp.name,
                                                        MAKE_ID_ONLY,
                                                        prop.name
                                                    )
                                                }
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
        if ((!prop.isAssociation(false) && !prop.isList) || prop.manyToManyViewBaseProp != null) {
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
                                        "if (!__isLoaded(%T.byIndex(%L)) || %L === null)",
                                        PROP_ID_CLASS_NAME,
                                        prop.slotName,
                                        prop.name
                                    )
                                } else {
                                    beginControlFlow(
                                        "if (!__isLoaded(%T.byIndex(%L)))",
                                        PROP_ID_CLASS_NAME,
                                        prop.slotName
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
                .addParameter("prop", argType)
                .addModifiers(KModifier.OVERRIDE)
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            val appender = CaseAppender(this, type, argType)
                            if (argType == PropId::class) {
                                beginControlFlow("when (prop.asIndex())")
                                appender.addIllegalCase()
                                addStatement("__unload(prop.asName())")
                            } else {
                                beginControlFlow("when (prop)")
                            }
                            for (prop in type.propsOrderById) {
                                appender.addCase(prop)
                                indent()
                                when {
                                    prop.baseProp !== null ->
                                        addStatement(
                                            "__unload(%T.byIndex(%L))",
                                            PROP_ID_CLASS_NAME,
                                            prop.baseProp!!.slotName
                                        )
                                    prop.isKotlinFormula ->
                                        addStatement("{}")
                                    prop.loadedFieldName !== null ->
                                        addStatement("%L\n.%L = false", MODIFIED, prop.loadedFieldName)
                                    else ->
                                        addStatement("%L\n.%L = null", MODIFIED, prop.valueFieldName)
                                }
                                unindent()
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
                .addParameter("prop", argType)
                .addParameter("value", ANY.copy(nullable = true))
                .addModifiers(KModifier.OVERRIDE)
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            val appender = CaseAppender(this, type, argType)
                            if (argType == PropId::class) {
                                beginControlFlow("when (prop.asIndex())")
                                appender.addIllegalCase()
                                addStatement("__set(prop.asName(), value)")
                            } else {
                                beginControlFlow("when (prop)")
                            }
                            for (prop in type.propsOrderById) {
                                appender.addCase(prop)
                                if (prop.isKotlinFormula || prop.manyToManyViewBaseProp != null) {
                                    add("return //%L is readonly, ignore\n", prop.name)
                                } else {
                                    add(
                                        "this.%L = value as %T?",
                                        prop.name,
                                        prop.typeName(overrideNullable = false)
                                    )
                                    if (!prop.isNullable) {
                                        add(
                                            "\n\t?: throw IllegalArgumentException(%S)",
                                            "'${prop.name} cannot be null"
                                        )
                                    }
                                    add("\n")
                                }
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
                .addParameter("prop", argType)
                .addParameter("visible", BOOLEAN)
                .addModifiers(KModifier.OVERRIDE)
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            val appender = CaseAppender(this, type, argType)
                            if (argType == PropId::class) {
                                beginControlFlow("when (prop.asIndex())")
                                appender.addIllegalCase()
                                addStatement("__show(prop.asName(), visible)")
                            } else {
                                beginControlFlow("when (prop)")
                            }
                            for (prop in type.propsOrderById) {
                                appender.addCase(prop)
                                addStatement("%L.__visibility.show(%L, visible)", MODIFIED, prop.slotName)
                            }
                            add("else -> throw IllegalArgumentException(\n")
                            indent()
                            add(
                                "%S + \nprop + \n%S",
                                "Illegal property " +
                                    (if (argType == String::class) "name" else "id") +
                                    ": \"",
                                "\",it does not exists"
                            )
                            unindent()
                            add("\n)\n")
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
                            addStatement("var __tmpModified = __modified")
                            if (type.properties.values.any {
                                    it.valueFieldName !== null && (it.isAssociation(false) || it.isList)
                            }) {
                                beginControlFlow("if (__tmpModified === null)")
                                for (prop in type.properties.values) {
                                    if (prop.valueFieldName !== null &&
                                        (prop.isAssociation(false) || prop.isList)
                                    ) {
                                        beginControlFlow(
                                            "if (__isLoaded(%T.byIndex(%L)))",
                                            PROP_ID_CLASS_NAME,
                                            prop.slotName
                                        )
                                        addStatement("val oldValue = base!!.%L", prop.name)
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
                                    if (prop.valueFieldName !== null) {
                                        if (prop.isList) {
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
                                }
                                endControlFlow()
                            }
                            beginControlFlow(
                                "if (base !== null && (__tmpModified === null || \n\t%T.equals(base, __tmpModified, true)))",
                                IMMUTABLE_SPI_CLASS_NAME
                            )
                            addStatement("return base")
                            endControlFlow()
                            for ((className, _) in type.validationMessages) {
                                addStatement("%L.validate(__tmpModified)", validatorFieldName(className))
                            }
                            addStatement("return __tmpModified!!")
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
                                        .initializer("%T.compile(%S)", PATTERN_CLASS_NAME, patterns[i][Pattern::regexp])
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
                                            "%T(%T::class.java, %S, %T::class.java, %T.byIndex(%L))",
                                            VALIDATOR_CLASS_NAME,
                                            className,
                                            message,
                                            type.className,
                                            PROP_ID_CLASS_NAME,
                                            prop.slotName
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