package org.babyfish.jimmer.ksp.util

import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.babyfish.jimmer.jackson.Converter
import org.babyfish.jimmer.ksp.MetaException

data class ConverterMetadata(
    val sourceTypeName: TypeName,
    val targetTypeName: TypeName
)

fun converterMetadataOf(declaration: KSClassDeclaration): ConverterMetadata {
    if (declaration.typeParameters.isNotEmpty()) {
        throw MetaException(
            declaration,
            "It should not have type parameters"
        )
    }
    return ParseContext(declaration).get()
}

private class ParseContext(private val declaration: KSClassDeclaration) {

    private val replaceMap: MutableMap<KSTypeParameter, KSTypeArgument> = HashMap()

    private fun parse(type: KSClassDeclaration, arguments: List<KSTypeArgument>) {
        if (type.qualifiedName!!.asString() == CONVERTER_NAME) {
            if (arguments.size == 2) {
                throw Finished(
                    resolve(arguments[0]).copy(nullable = false),
                    resolve(arguments[1]).copy(nullable = false)
                )
            }
        } else {
            if (arguments.isNotEmpty()) {
                val parameters = type.typeParameters
                val size = arguments.size
                for (i in 0 until size) {
                    replaceMap[parameters[i]] = arguments[i]
                }
            }
            for (superTypeRef in type.superTypes) {
                val superType = superTypeRef.resolve()
                parse(
                    superType.declaration as KSClassDeclaration,
                    superType.arguments
                )
            }
        }
    }

    fun get(): ConverterMetadata {
        try {
            parse(declaration, listOf())
        } catch (ex: Finished) {
            return ConverterMetadata(ex.sourceTypeName, ex.targetTypeName)
        }
        throw MetaException(
            declaration,
            "it does not specify the arguments for \"" +
                Converter::class.java.getName() +
                "\""
        )
    }

    private fun resolve(type: KSType): TypeName =
        when (type) {
            is KSTypeParameter -> {
                val replaced = replaceMap[type]
                    ?: throw MetaException(
                        declaration,
                        "The type parameter \"$type\" cannot be resolved"
                    )
                resolve(replaced)
            }
            is KSTypeArgument ->
                resolve(type as KSTypeArgument)
            else ->
                ClassName.bestGuess(
                    type.declaration.qualifiedName!!.asString().let {
                        when (it) {
                            "kotlin.collections.MutableCollection" -> "kotlin.collections.Collection"
                            "kotlin.collections.MutableList" -> "kotlin.collections.List"
                            "kotlin.collections.MutableSet" -> "kotlin.collections.Set"
                            "kotlin.collections.MutableMap" -> "kotlin.collections.Map"
                            else -> it
                        }
                    }
                ).let {
                if (type.arguments.isEmpty()) {
                    it
                } else {
                    it.parameterizedBy(
                        type.arguments.map { a -> resolve(a) }
                    )
                }.copy(nullable = type.isMarkedNullable)
            }
        }

    private fun resolve(arg: KSTypeArgument): TypeName =
        when (arg.variance) {
            Variance.STAR -> WildcardTypeName.producerOf(ANY.copy(nullable = true))
            Variance.COVARIANT ->
                WildcardTypeName.producerOf(resolve(arg.type!!.resolve()))
            Variance.CONTRAVARIANT ->
                WildcardTypeName.consumerOf(resolve(arg.type!!.resolve()))
            else ->
                resolve(arg.type!!.resolve())
        }

    companion object {
        private val CONVERTER_NAME = Converter::class.java.getName()
    }
}

private class Finished(
    val sourceTypeName: TypeName,
    val targetTypeName: TypeName
) : Exception()
