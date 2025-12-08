package org.babyfish.jimmer.ksp.dto

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import org.babyfish.jimmer.dto.compiler.DtoModifier
import org.babyfish.jimmer.dto.compiler.DtoType
import org.babyfish.jimmer.impl.util.StringUtil
import org.babyfish.jimmer.ksp.immutable.generator.JSON_GENERATOR_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.JSON_SERIALIZER_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.generator.SERIALIZER_PROVIDER_CLASS_NAME
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType

class SerializerGenerator(
    private val parentGenerator: DtoGenerator
) {
    private val dtoType: DtoType<ImmutableType, ImmutableProp> =
        parentGenerator.dtoType

    fun generate() {
        parentGenerator.typeBuilder.addType(
            TypeSpec.classBuilder("Serializer")
                .superclass(
                    JSON_SERIALIZER_CLASS_NAME.parameterizedBy(
                        parentGenerator.getDtoClassName()
                    )
                )
                .addFunction(newSerialize())
                .build()
        )
    }

    private fun newSerialize(): FunSpec =
        FunSpec.builder("serialize")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(
                "input",
                    parentGenerator.getDtoClassName()
            )
            .addParameter(
                "gen",
                JSON_GENERATOR_CLASS_NAME
            )
            .addParameter(
                "provider",
                SERIALIZER_PROVIDER_CLASS_NAME
            )
            .addStatement("gen.writeStartObject()")
            .apply {
                for (prop in dtoType.dtoProps) {
                    if (prop.inputModifier == DtoModifier.DYNAMIC) {
                        beginControlFlow(
                            "if (input.%L)",
                            StringUtil.identifier("is", prop.name, "Loaded")
                        )
                        addStatement(
                            "provider.defaultSerializeField(%S, input.%L, gen)",
                            prop.name,
                            prop.name
                        )
                        endControlFlow()
                    } else {
                        addStatement(
                            "provider.defaultSerializeField(%S, input.%L, gen)",
                            prop.name,
                            prop.name
                        )
                    }
                }
            }
            .addStatement("gen.writeEndObject()")
            .build()
}