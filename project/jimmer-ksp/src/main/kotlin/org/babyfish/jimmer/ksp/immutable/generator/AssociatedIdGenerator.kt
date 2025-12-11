package org.babyfish.jimmer.ksp.immutable.generator

import com.squareup.kotlinpoet.*
import org.babyfish.jimmer.impl.util.StringUtil
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp

internal class AssociatedIdGenerator(
    private val ctx: Context,
    private val typeBuilder: TypeSpec.Builder,
    private val withImplementation: Boolean
) {

    fun generate(prop: ImmutableProp) {
        val name = StringUtil.identifier(prop.name, "Id")
        if (!prop.isAssociation(true) ||
            prop.isList ||
            prop.idViewProp != null ||
            prop.declaringType.properties.containsKey(name)
        ) {
            return
        }
        val targetType = prop.targetType!!
        val associatedIdProp = targetType.idProp!!
        typeBuilder.addProperty(
            PropertySpec
                .builder(
                    name,
                    associatedIdProp.typeName(overrideNullable = prop.isNullable)
                )
                .addModifiers(KModifier.PUBLIC)
                .addAnnotation(
                    AnnotationSpec.builder(ctx.jacksonTypes.jsonIgnore)
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                        .build()
                )
                .apply {
                    if (withImplementation) {
                        addModifiers(KModifier.OVERRIDE)
                    } else {
                        addModifiers(KModifier.ABSTRACT)
                    }
                }
                .mutable(true)
                .apply {
                    if (withImplementation) {
                        getter(
                            FunSpec
                                .getterBuilder()
                                .addStatement(
                                    "return %L%L%L",
                                    prop.name,
                                    if (prop.isNullable) "?." else ".",
                                    associatedIdProp.name
                                )
                                .build()
                        )
                        setter(
                            FunSpec
                                .setterBuilder()
                                .addParameter(
                                    ParameterSpec(
                                        name,
                                        associatedIdProp.typeName(overrideNullable = prop.isNullable)
                                    )
                                )
                                .apply {
                                    if (prop.isNullable) {
                                        beginControlFlow("if (%L === null)", name)
                                        addStatement("this.%L = null", prop.name)
                                        addStatement("return")
                                        endControlFlow()
                                    }
                                    addStatement("%L().%L = %L", prop.name, associatedIdProp.name, name)
                                }
                                .build()
                        )
                    }
                }
                .build()
        )
    }
}