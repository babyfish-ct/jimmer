package org.babyfish.jimmer.ksp.util

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.jackson.Converter
import org.babyfish.jimmer.ksp.MetaException
import java.lang.IllegalStateException

open class ConverterMetadata(
    val sourceType: KSTypeArgument,
    val targetType: KSTypeArgument,
    val sourceTypeName: TypeName,
    val targetTypeName: TypeName
) {
    open fun toListMetadata(resolver: Resolver): ConverterMetadata =
        ListMetadata(
            resolver.getTypeArgument(
                resolver.createKSTypeReferenceFromKSType(
                    resolver
                        .getClassDeclarationByName("kotlin.collections.List")!!
                        .asType(listOf(sourceType))
                    ),
                Variance.INVARIANT
            ),
            resolver.getTypeArgument(
                resolver.createKSTypeReferenceFromKSType(
                    resolver
                        .getClassDeclarationByName("kotlin.collections.List")!!
                        .asType(listOf(targetType))
                ),
                Variance.INVARIANT
            ),
            LIST.parameterizedBy(sourceTypeName),
            LIST.parameterizedBy(targetTypeName)
        )

    override fun hashCode(): Int {
        return sourceTypeName.hashCode() xor targetTypeName.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || this::class != other::class) {
            return false
        }
        val metadata = other as ConverterMetadata
        return sourceTypeName == metadata.sourceTypeName &&
            targetTypeName == metadata.targetTypeName
    }

    override fun toString(): String {
        return "ConverterMetadata(sourceType = $sourceTypeName, targetType = $targetTypeName)"
    }

    private class ListMetadata(
        sourceType: KSTypeArgument,
        targetType: KSTypeArgument,
        sourceTypeName: TypeName,
        targetTypeName: TypeName
    ) : ConverterMetadata(
        sourceType,
        targetType,
        sourceTypeName,
        targetTypeName
    ) {
        override fun toListMetadata(resolver: Resolver): ConverterMetadata =
            throw IllegalStateException("The current metadata is already list metadata")
    }
}

fun Resolver.converterMetadataOf(declaration: KSClassDeclaration): ConverterMetadata {
    if (declaration.typeParameters.isNotEmpty()) {
        throw MetaException(
            declaration,
            "It should not have type parameters"
        )
    }
    val result = GenericParser("converter", declaration, Converter::class.qualifiedName.toString()).parse()
    return ConverterMetadata(
        result.arguments[0],
        result.arguments[1],
        result.argumentTypeNames[0],
        result.argumentTypeNames[1]
    )
}
