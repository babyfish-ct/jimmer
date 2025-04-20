package org.babyfish.jimmer.ksp.dto

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import org.babyfish.jimmer.dto.compiler.DtoModifier
import org.babyfish.jimmer.dto.compiler.DtoType
import org.babyfish.jimmer.impl.util.StringUtil
import org.babyfish.jimmer.ksp.immutable.generator.JSON_SERIALIZER_CLASS_NAME
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
                JsonGenerator::class.asClassName()
            )
            .addParameter(
                "provider",
                SerializerProvider::class.asClassName()
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