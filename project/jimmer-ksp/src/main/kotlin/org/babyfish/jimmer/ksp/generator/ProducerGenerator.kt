package org.babyfish.jimmer.ksp.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType
import org.babyfish.jimmer.sql.*

class ProducerGenerator(
    private val type: ImmutableType,
    private val parent: TypeSpec.Builder
) {
    fun generate() {
        parent.addType(
            TypeSpec
                .objectBuilder(PRODUCER)
                .apply {
                    if (!type.isMappedSuperclass) {
                        addSlots()
                    }
                    addTypeProp()
                    if (!type.isMappedSuperclass) {
                        addProduceFun()
                        ImplementorGenerator(type, this).generate()
                        ImplGenerator(type, this).generate()
                        DraftImplGenerator(type, this).generate()
                    }
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addTypeProp() {
        addProperty(
            PropertySpec
                .builder(
                    "type",
                    IMMUTABLE_TYPE_CLASS_NAME
                )
                .initializer(
                    CodeBlock
                        .builder()
                        .apply { initializeType() }
                        .build()
                )
                .build()
        )
    }

    private fun CodeBlock.Builder.initializeType() {
        add("%T\n", IMMUTABLE_TYPE_CLASS_NAME)
        indent()
        add(".newBuilder(\n")
        indent()
        add("%T::class,\n", type.className)
        refSuperType()
        add(",\n")
        if (type.isMappedSuperclass) {
            add("null")
            unindent()
            add("\n)")
        } else {
            unindent()
            add(") { ctx, base ->\n")
            indent()
            addStatement("%T(ctx, base as %T?)", type.draftClassName(PRODUCER, DRAFT_IMPL), type.className)
            unindent()
            add("}")
        }
        add("\n")
        if (!type.isMappedSuperclass) {
            for (prop in type.redefinedProps.values) {
                add(".redefine(%S, %L)\n", prop.name, prop.slotName)
            }
        }
        for (prop in type.declaredProperties.values) {
            addProp(prop)
        }
        add(".build()")
        unindent()
    }

    private fun CodeBlock.Builder.refSuperType() {
        add("listOf(\n")
        indent()
        for (index in type.superTypes.indices) {
            if (index != 0) {
                add(",\n")
            }
            add("%T.type", type.superTypes[index].draftClassName("$"))
        }
        unindent()
        add("\n)")
    }

    private fun CodeBlock.Builder.addProp(prop: ImmutableProp) {
        val propId = if (type.isMappedSuperclass) "-1" else prop.slotName
        when {
            prop.primaryAnnotationType == Id::class.java ->
                add(
                    ".id(%L, %S, %T::class.java)\n",
                    propId,
                    prop.name,
                    prop.targetClassName
                )
            prop.primaryAnnotationType == Version::class.java ->
                add(
                    ".version(%L, %S)\n",
                    propId,
                    prop.name
                )
            prop.primaryAnnotationType == LogicalDeleted::class.java ->
                add(
                    ".logicalDeleted(%L, %S, %T::class.java, %L)\n",
                    propId,
                    prop.name,
                    prop.targetClassName,
                    prop.isNullable
                )
            prop.isKey && prop.isAssociation(false) ->
                add(
                    ".keyReference(%L, %S, %T::class.java, %T::class.java, %L)\n",
                    propId,
                    prop.name,
                    if (prop.annotation(OneToOne::class) !== null) {
                        OneToOne::class
                    } else {
                        ManyToOne::class
                    },
                    prop.targetClassName,
                    prop.isNullable
                )
            prop.isKey && !prop.isAssociation(false) ->
                add(
                    ".key(%L, %S, %T::class.java, %L)\n",
                    propId,
                    prop.name,
                    prop.targetClassName,
                    prop.isNullable
                )
            prop.primaryAnnotationType == IdView::class.java ->
                add(
                    ".add(%L, %S, %T.%L, %T::class.java, %L)",
                    propId,
                    prop.name,
                    IMMUTABLE_PROP_CATEGORY_CLASS_NAME,
                    if (prop.isList) "SCALAR_LIST" else "SCALAR",
                    prop.targetClassName,
                    prop.isNullable
                )
            prop.primaryAnnotationType != null && prop.primaryAnnotationType != Formula::class.java && prop.primaryAnnotationType != Transient::class.java ->
                add(
                    ".add(%L, %S, %T::class.java, %T::class.java, %L)\n",
                    propId,
                    prop.name,
                    when {
                        prop.primaryAnnotationType == OneToOne::class.java -> ONE_TO_ONE_CLASS_NAME
                        prop.primaryAnnotationType == ManyToOne::class.java -> MANY_TO_ONE_CLASS_NAME
                        prop.primaryAnnotationType == OneToMany::class.java -> ONE_TO_MANY_CLASS_NAME
                        prop.primaryAnnotationType == ManyToMany::class.java -> MANY_TO_MANY_CLASS_NAME
                        prop.primaryAnnotationType == ManyToManyView::class.java -> MANY_TO_MANY_VIEW_CLASS_NAME
                        else -> error("Internal bug: $prop has wrong sql annotation @${prop.primaryAnnotationType.name}")
                    },
                    prop.targetClassName,
                    prop.isNullable
                )
            else ->
                add(
                    ".add(%L, %S, %T.%L, %T::class.java, %L)\n",
                    propId,
                    prop.name,
                    IMMUTABLE_PROP_CATEGORY_CLASS_NAME,
                    when {
                        prop.isList && prop.isAssociation(false) -> "REFERENCE_LIST"
                        prop.isList && !prop.isAssociation(false) -> "SCALAR_LIST"
                        prop.isAssociation(false) -> "REFERENCE"
                        else -> "SCALAR"
                    },
                    prop.targetClassName,
                    prop.isNullable
                )
        }
    }

    private fun TypeSpec.Builder.addProduceFun() {
        addFunction(
            FunSpec
                .builder("produce")
                .addParameter(
                    ParameterSpec
                        .builder(
                            "base",
                            type.className.copy(nullable = true)
                        )
                        .defaultValue("null")
                        .build()
                )
                .addParameter(
                    ParameterSpec
                        .builder(
                            "block",
                            LambdaTypeName.get(
                                type.draftClassName,
                                emptyList(),
                                UNIT
                            )
                        )
                        .build()
                )
                .returns(type.className)
                .addStatement(
                    "val consumer = %T { block(it) }",
                    DRAFT_CONSUMER_CLASS_NAME.parameterizedBy(
                        type.draftClassName
                    )
                )
                .addStatement(
                    "return %T.produce(type, base, consumer) as %T",
                    INTERNAL_TYPE_CLASS_NAME,
                    type.className
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addSlots() {
        for (prop in type.properties.values) {
            addProperty(
                PropertySpec
                    .builder(
                        prop.slotName,
                        INT,
                        KModifier.CONST
                    )
                    .apply {
                        if (prop.declaringType == type || prop.declaringType.isMappedSuperclass) {
                            initializer(prop.id.toString())
                        } else {
                            initializer("%T.%L", prop.declaringType.draftClassName("$"), prop.slotName)
                        }
                    }
                    .build()
            )
        }
    }
}