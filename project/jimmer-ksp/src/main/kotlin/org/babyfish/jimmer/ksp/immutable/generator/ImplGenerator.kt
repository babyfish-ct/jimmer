package org.babyfish.jimmer.ksp.immutable.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType
import org.babyfish.jimmer.ksp.name
import org.babyfish.jimmer.ksp.util.generatedAnnotation
import org.babyfish.jimmer.meta.PropId
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
                .addAnnotation(generatedAnnotation(type))
                .addSuperinterface(type.draftClassName(PRODUCER, IMPLEMENTOR))
                .addSuperinterface(CLONEABLE_CLASS_NAME)
                .addSuperinterface(SERIALIZABLE_CLASS_NAME)
                .apply {
                    addProperty(
                        PropertySpec
                            .builder("__visibility", VISIBILITY_CLASS_NAME.copy(nullable = true))
                            .addModifiers(KModifier.INTERNAL)
                            .mutable()
                            .addAnnotation(
                                AnnotationSpec
                                    .builder(JSON_IGNORE_CLASS_NAME)
                                    .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                                    .build()
                            )
                            .initializer("null")
                            .build()
                    )
                    for (prop in type.properties.values) {
                        addFields(prop)
                    }
                    addInit()
                    for (prop in type.properties.values) {
                        addProp(prop)
                    }
                    addCloneFun()
                    addIsLoadedFun(PropId::class)
                    addIsLoadedFun(String::class)
                    addIsVisibleFun(PropId::class)
                    addIsVisibleFun(String::class)
                    addHashCodeFun(true)
                    addHashCodeFun(false)
                    addParameterizedHashCode()
                    addEqualsFun(true)
                    addEqualsFun(false)
                    addParameterizedEquals()
                    addToStringFun()
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
                    .addAnnotation(
                        AnnotationSpec
                            .builder(JSON_IGNORE_CLASS_NAME)
                            .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                            .build()
                    )
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
                    .addAnnotation(
                        AnnotationSpec
                            .builder(JSON_IGNORE_CLASS_NAME)
                            .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                            .build()
                    )
                    .initializer("false")
                    .mutable()
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.addInit() {

        if (type.properties.values.all { it.valueFieldName !== null }) {
            return
        }

        addFunction(
            FunSpec
                .constructorBuilder()
                .apply {
                    if (type.properties.values.any { it.valueFieldName == null }) {
                        addStatement("val __visibility = %T.of(%L)", VISIBILITY_CLASS_NAME, type.properties.size)
                        for (prop in type.properties.values) {
                            if (prop.valueFieldName === null) {
                                addStatement("__visibility.show(%L, false)", prop.slotName)
                            }
                        }
                        addStatement("this.__visibility = __visibility")
                    }
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addProp(prop: ImmutableProp) {
        if (prop.isKotlinFormula) {
            return
        }
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
                                    val idViewBaseProp = prop.idViewBaseProp
                                    val manyToManyViewBaseProp = prop.manyToManyViewBaseProp
                                    when {
                                        idViewBaseProp !== null ->
                                            if (prop.isList) {
                                                addStatement(
                                                    "return %T(%T.type, %L)",
                                                    ID_VIEW_CLASS_NAME,
                                                    idViewBaseProp.targetType!!.draftClassName("$"),
                                                    idViewBaseProp.name
                                                )
                                            } else {
                                                addStatement(
                                                    "return %N%L%N",
                                                    idViewBaseProp.name,
                                                    if (prop.isNullable) "?." else ".",
                                                    idViewBaseProp.targetType!!.idProp!!.name
                                                )
                                            }
                                        manyToManyViewBaseProp !== null ->
                                            addStatement(
                                                "return %T(%T.byIndex(%T.%L), %N)",
                                                MANY_TO_MANY_VIEW_LIST_CLASS_NAME,
                                                PROP_ID_CLASS_NAME,
                                                prop.manyToManyViewBaseDeeperProp!!.declaringType.draftClassName("$"),
                                                prop.manyToManyViewBaseDeeperProp!!.slotName,
                                                manyToManyViewBaseProp.name
                                            )
                                        else -> {
                                            if (prop.loadedFieldName === null) {
                                                addStatement("val %N = this.%N", prop.valueFieldName, prop.valueFieldName)
                                            }
                                            beginControlFlow(
                                                when {
                                                    prop.loadedFieldName !== null -> "if (!${prop.loadedFieldName})"
                                                    else -> "if (${prop.valueFieldName} ${if (prop.isUnsigned) "==" else "==="} null)"
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
                .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
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
                .addParameter("prop", argType)
                .returns(BOOLEAN)
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            val appender = CaseAppender(this, type, argType)
                            add("return ")
                            if (argType == PropId::class) {
                                beginControlFlow("when (prop.asIndex())")
                                appender.addIllegalCase()
                                addStatement("__isLoaded(prop.asName())")
                            } else {
                                beginControlFlow("when (prop)")
                            }
                            for (prop in type.propsOrderById) {
                                val idViewBaseProp = prop.idViewBaseProp
                                val manyToManyViewBaseProp = prop.manyToManyViewBaseProp
                                appender.addCase(prop)
                                when {
                                    idViewBaseProp !== null ->
                                        if (prop.isList) {
                                            addStatement(
                                                "__isLoaded(%T.byIndex(%L)) && \n%L.all { (it as %T).__isLoaded(%T.byIndex(%T.%L)) }",
                                                PROP_ID_CLASS_NAME,
                                                idViewBaseProp.slotName,
                                                idViewBaseProp.name,
                                                IMMUTABLE_SPI_CLASS_NAME,
                                                PROP_ID_CLASS_NAME,
                                                idViewBaseProp.targetType!!.draftClassName("$"),
                                                idViewBaseProp.targetType!!.idProp!!.slotName
                                            )
                                        } else {
                                            addStatement(
                                                "__isLoaded(%T.byIndex(%L)) && \n(%L as %T)%L__isLoaded(%T.byIndex(%T.%L)) ?: true",
                                                PROP_ID_CLASS_NAME,
                                                idViewBaseProp.slotName,
                                                idViewBaseProp.name,
                                                IMMUTABLE_SPI_CLASS_NAME.copy(nullable = idViewBaseProp.isNullable),
                                                if (idViewBaseProp.isNullable) "?." else ".",
                                                PROP_ID_CLASS_NAME,
                                                idViewBaseProp.targetType!!.draftClassName("$"),
                                                idViewBaseProp.targetType!!.idProp!!.slotName
                                            )
                                        }
                                    manyToManyViewBaseProp !== null ->
                                        addStatement(
                                            "__isLoaded(%T.byIndex(%L)) && \n%L.all { (it as %T).__isLoaded(%T.byIndex(%T.%L)) }",
                                            PROP_ID_CLASS_NAME,
                                            manyToManyViewBaseProp.slotName,
                                            manyToManyViewBaseProp.name,
                                            IMMUTABLE_SPI_CLASS_NAME,
                                            PROP_ID_CLASS_NAME,
                                            prop.manyToManyViewBaseDeeperProp!!.declaringType.draftClassName("$"),
                                            prop.manyToManyViewBaseDeeperProp!!.slotName
                                        )
                                    prop.isKotlinFormula -> {
                                        indent()
                                        var first = true
                                        for (dependency in prop.dependencies) {
                                            if (first) {
                                                first = false
                                            } else {
                                                add(" && \n")
                                            }
                                            if (dependency.props.size == 1) {
                                                add(
                                                    "__isLoaded(%T.byIndex(%L))",
                                                    PROP_ID_CLASS_NAME,
                                                    dependency.props[0].slotName
                                                )
                                            } else {
                                                add("%T.isLoadedChain(this", IMMUTABLE_OBJECTS_CLASS_NAME)
                                                for (depProp in dependency.props) {
                                                    add(
                                                        ", %T.byIndex(%T.%L)",
                                                        PROP_ID_CLASS_NAME,
                                                        depProp.declaringType.draftClassName("$"),
                                                        depProp.slotName
                                                    )
                                                }
                                                add(")")
                                            }
                                        }
                                        add("\n")
                                        unindent()
                                    }
                                    else -> {
                                        val cond = prop.loadedFieldName ?: "${prop.valueFieldName} ${if (prop.isUnsigned) "!=" else "!=="} null"
                                        addStatement("%L", cond)
                                    }
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

    private fun TypeSpec.Builder.addIsVisibleFun(argType: KClass<*>) {
        addFunction(
            FunSpec
                .builder("__isVisible")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("prop", argType)
                .returns(BOOLEAN)
                .addCode(
                    CodeBlock
                        .builder()
                        .apply {
                            addStatement("val __visibility = this.__visibility ?: return true")
                            val appender = CaseAppender(this, type, argType)
                            add("return ")
                            if (argType == PropId::class) {
                                beginControlFlow("when (prop.asIndex())")
                                appender.addIllegalCase()
                                addStatement("__isVisible(prop.asName())")
                            } else {
                                beginControlFlow("when (prop)")
                            }
                            for (prop in type.propsOrderById) {
                                appender.addCase(prop)
                                addStatement("__visibility.visible(%L)", prop.slotName)
                            }
                            addStatement("else -> true")
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
                            addStatement("var hash = __visibility?.hashCode() ?: 0")
                            for (prop in type.properties.values) {
                                if (prop.valueFieldName === null) {
                                    continue
                                }
                                beginControlFlow(
                                    "if (%L)",
                                    prop.loadedFieldName ?: "${prop.valueFieldName} ${if (prop.isUnsigned) "!=" else "!=="} null"
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
                                beginControlFlow(
                                    "if (__isVisible(%T.byIndex(%L)) != __other.__isVisible(%T.byIndex(%L)))",
                                    PROP_ID_CLASS_NAME,
                                    prop.slotName,
                                    PROP_ID_CLASS_NAME,
                                    prop.slotName
                                )
                                addStatement("return false")
                                endControlFlow()
                                if (prop.valueFieldName == null) {
                                    continue
                                }
                                val localLoadedName = "__${prop.name}Loaded"
                                val objLoadedName = prop.loadedFieldName ?: "${prop.valueFieldName} ${if (prop.isUnsigned) "!=" else "!=="} null"
                                add("val %L = \n", localLoadedName)
                                addStatement("    this.%L", objLoadedName)
                                beginControlFlow(
                                    "if (%L != (__other.__isLoaded(%T.byIndex(%L))))",
                                    localLoadedName,
                                    PROP_ID_CLASS_NAME,
                                    prop.slotName
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

    private fun TypeSpec.Builder.addToStringFun() {
        addFunction(
            FunSpec
                .builder("toString")
                .addModifiers(KModifier.OVERRIDE)
                .returns(STRING)
                .addCode("return %T.toString(this)", IMMUTABLE_OBJECTS_CLASS_NAME)
                .build()
        )
    }
}