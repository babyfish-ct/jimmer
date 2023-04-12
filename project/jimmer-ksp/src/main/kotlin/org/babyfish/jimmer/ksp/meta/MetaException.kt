package org.babyfish.jimmer.ksp.meta

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class MetaException(
    val declaration: KSDeclaration,
    reason: String,
    cause: Throwable? = null
) : RuntimeException(message(declaration, reason), cause) {

    companion object {

        @JvmStatic
        private fun message(declaration: KSDeclaration, reason: String): String =
            when (declaration ){
                is KSClassDeclaration ->
                    "Illegal type \"" +
                        declaration.qualifiedName!!.asString() +
                        "\", " +
                        lowerFirstChar(reason)
                is KSPropertyDeclaration ->
                    "Illegal property \"" +
                        declaration.parentDeclaration!!.qualifiedName!!.asString() +
                        '.' +
                        declaration.simpleName.asString() +
                        "\", " +
                        lowerFirstChar(reason)
                is KSFunctionDeclaration ->
                    "Illegal function \"" +
                        declaration.parentDeclaration!!.qualifiedName!!.asString() +
                        '.' +
                        declaration.simpleName.asString() +
                        "\", " +
                        lowerFirstChar(reason)
                else ->
                    reason
            }

        @JvmStatic
        private fun lowerFirstChar(reason: String): String =
            if (reason[0].isWhitespace()) {
                reason.trimStart()
            } else {
                reason
            }.let {
                if (it[0].isUpperCase()) {
                    it[0].lowercase() + it.substring(1)
                } else {
                    it
                }
            }
    }
}