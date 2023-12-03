package org.babyfish.jimmer.ksp

import com.google.devtools.ksp.symbol.*
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

class MetaException(
    val declaration: KSDeclaration,
    childDeclaration: KSDeclaration?,
    reason: String,
    cause: Throwable? = null
) : RuntimeException(
    message(
        declaration,
        if (childDeclaration === null || childDeclaration === declaration) {
            reason
        } else {
            message(childDeclaration, reason)
        }
    ),
    cause
) {

    constructor(
        declaration: KSDeclaration,
        reason: String,
        cause: Throwable? = null
    ): this(
        declaration, null, reason, cause
    )

    companion object {

        @JvmStatic
        private fun message(declaration: KSDeclaration, reason: String): String =
            when (declaration ){
                is KSClassDeclaration ->
                    "Illegal type \"" +
                        longName(declaration) +
                        "\", " +
                        lowerFirstChar(reason)
                is KSPropertyDeclaration ->
                    "Illegal property \"" +
                        longName(declaration) +
                        "\", " +
                        lowerFirstChar(reason)
                is KSFunctionDeclaration ->
                    "Illegal function \"" +
                        longName(declaration) +
                        "\", " +
                        lowerFirstChar(reason)
                else ->
                    reason
            }

        @JvmStatic
        private fun longName(declaration: KSDeclaration): String {
            return if (declaration is KSClassDeclaration) {
                declaration.qualifiedName!!.asString()
            } else longName(declaration.parentDeclaration!!) +
                (if (declaration is KSValueParameter) ':' else '.') +
                declaration.simpleName.asString()
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