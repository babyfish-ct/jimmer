package org.babyfish.jimmer.ksp.util

import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.jackson.Converter
import org.babyfish.jimmer.ksp.MetaException
import java.lang.IllegalStateException

open class ConverterMetadata(
    val sourceTypeName: TypeName,
    val targetTypeName: TypeName
) {
    open fun toListMetadata(): ConverterMetadata =
        ListMetadata(sourceTypeName, targetTypeName)

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
        sourceTypeName: TypeName,
        targetTypeName: TypeName
    ) : ConverterMetadata(
        LIST.parameterizedBy(
            sourceTypeName
        ),
        LIST.parameterizedBy(
            targetTypeName
        )
    ) {
        override fun toListMetadata(): ConverterMetadata =
            throw IllegalStateException("The current metadata is already list metadata")
    }
}

fun converterMetadataOf(declaration: KSClassDeclaration): ConverterMetadata {
    if (declaration.typeParameters.isNotEmpty()) {
        throw MetaException(
            declaration,
            "It should not have type parameters"
        )
    }
    val arguments = GenericParser("converter", declaration, Converter::class.qualifiedName.toString()).parse()
    return ConverterMetadata(arguments[0], arguments[1])
}
