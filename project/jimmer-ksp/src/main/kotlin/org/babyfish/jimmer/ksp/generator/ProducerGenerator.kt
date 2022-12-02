package org.babyfish.jimmer.ksp.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
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
                    addTypeProp()
                    addProduceFun()
                    ImplementorGenerator(type, this).generate()
                    ImplGenerator(type, this).generate()
                    DraftImplGenerator(type, this).generate()
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
        unindent()
        add("\n) { ctx, base ->\n")
        indent()
        addStatement("%T(ctx, base as %T?)", type.draftClassName(PRODUCER, DRAFT_IMPL), type.className)
        unindent()
        add("}\n")
        for (prop in type.declaredProperties.values) {
            addProp(prop)
        }
        add(".build()")
        unindent()
    }

    private fun CodeBlock.Builder.refSuperType() {
        val superType = type.superType
        if (superType !== null) {
            add("%T.type", superType.draftClassName("$"))
        } else {
            add("null")
        }
    }

    private fun CodeBlock.Builder.addProp(prop: ImmutableProp) {
        when {
            prop.primaryAnnotationType == Id::class.java ->
                add(
                    ".id(%L, %S, %T::class.java)\n",
                    prop.id,
                    prop.name,
                    prop.targetTypeName(overrideNullable = false)
                )
            prop.primaryAnnotationType == Version::class.java ->
                add(
                    ".version(%L, %S)\n",
                    prop.id,
                    prop.name
                )
            prop.isKey && prop.isAssociation(false) ->
                add(
                    ".keyReference(%L, %S, %T::class.java, %L)\n",
                    prop.id,
                    prop.name,
                    prop.targetTypeName(overrideNullable = false),
                    prop.isNullable
                )
            prop.isKey && !prop.isAssociation(false) ->
                add(
                    ".key(%L, %S, %T::class.java)\n",
                    prop.id,
                    prop.name,
                    prop.targetTypeName(overrideNullable = false)
                )
            prop.primaryAnnotationType !== null && prop.primaryAnnotationType != Transient::class.java ->
                add(
                    ".add(%L, %S, %T::class.java, %T::class.java, %L)\n",
                    prop.id,
                    prop.name,
                    when {
                        prop.primaryAnnotationType == OneToOne::class.java -> ONE_TO_ONE_CLASS_NAME
                        prop.primaryAnnotationType == ManyToOne::class.java -> MANY_TO_ONE_CLASS_NAME
                        prop.primaryAnnotationType == OneToMany::class.java -> ONE_TO_MANY_CLASS_NAME
                        prop.primaryAnnotationType == ManyToMany::class.java -> MANY_TO_MANY_CLASS_NAME
                        else -> error("Internal bug: $prop has wrong sql annotation @${prop.primaryAnnotationType.name}")
                    },
                    prop.targetTypeName(overrideNullable = false),
                    prop.isNullable
                )
            else ->
                add(
                    ".add(%L, %S, %T.%L, %T::class.java, %L)\n",
                    prop.id,
                    prop.name,
                    IMMUTABLE_PROP_CATEGORY_CLASS_NAME,
                    when {
                        prop.isList && prop.isAssociation(false) -> "REFERENCE_LIST"
                        prop.isList && !prop.isAssociation(false) -> "SCALAR_LIST"
                        prop.isAssociation(false) -> "REFERENCE"
                        else -> "SCALAR"
                    },
                    prop.targetTypeName(overrideNullable = false),
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
}