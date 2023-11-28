package org.babyfish.jimmer.ksp.util

import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import org.babyfish.jimmer.ksp.MetaException
import java.lang.Exception
import java.lang.IllegalStateException

class GenericParser(
    private val name: String,
    private val declaration: KSClassDeclaration,
    private val superName: String
) {

    private val replaceMap: MutableMap<KSTypeParameter, KSTypeArgument> = mutableMapOf()

    init {
        if (declaration.typeParameters.isNotEmpty()) {
            throw IllegalStateException("\"${declaration.qualifiedName!!.asString()}\" cannot have type parameters");
        }
    }

    fun parse(): List<TypeName> {
        try {
            parse(declaration.asType(emptyList()))
        } catch (ex: Finished) {
            return ex.arguments
        }
        throw MetaException(
            declaration,
            "it does not specify the arguments for \"" +
                superName +
                "\""
        )
    }

    private fun parse(type: KSType) {
        if (type.declaration.qualifiedName!!.asString() == superName) {
            if (type.arguments.isEmpty()) {
                throw MetaException(
                    declaration,
                    "it does not specify type argument for \"$superName\""
                )
            }
            throw Finished(
                type.arguments.map { resolve(it) }
            )
        } else {
            if (type.arguments.isNotEmpty()) {
                val parameters = type.declaration.typeParameters
                val size = parameters.size
                for (i in 0 until size) {
                    replaceMap[parameters[i]] = type.arguments[i]
                }
            }
            for (superTypeRef in (type.declaration as KSClassDeclaration).superTypes) {
                val superType = superTypeRef.resolve()
                parse(superType)
            }
        }
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
                            "kotlin.collections.MutableIterable" -> "kotlin.collections.Iterable"
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

    private class Finished(
        val arguments: List<TypeName>
    ) : Exception()
}