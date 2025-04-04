package org.babyfish.jimmer.ksp.transactional

import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.*
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.MetaException
import org.babyfish.jimmer.ksp.annotation
import org.babyfish.jimmer.ksp.fullName
import org.babyfish.jimmer.ksp.util.fastResolve

class TxProcessor(
    val ctx: Context
) {
    fun process() {
        if (ctx.isBuddyIgnoreResourceGeneration) {
            return
        }
        val map = mutableMapOf<String, KSClassDeclaration>()
        for (file in ctx.resolver.getNewFiles()) {
            for (declaration in file.declarations) {
                if (declaration is KSClassDeclaration) {
                    if (isTxType(declaration)) {
                        validateType(declaration)
                        map[declaration.fullName] = declaration
                    }
                }
            }
        }
        if (map.isEmpty()) {
            return
        }
        val allFiles = ctx.resolver.getAllFiles().toList()
        for (declaration in map.values) {
            TxGenerator(ctx.environment.codeGenerator, ctx, declaration).generate(allFiles)
        }
    }

    private fun isTxType(declaration: KSClassDeclaration): Boolean {
        if (declaration.annotation(TX) !== null) {
            return true
        }
        for (subDeclaration in declaration.declarations) {
            if (subDeclaration.annotation(TX) !== null) {
                if (subDeclaration !is KSFunctionDeclaration || subDeclaration.functionKind != FunctionKind.MEMBER || subDeclaration.isConstructor()) {
                    throw MetaException(
                        subDeclaration,
                        "it cannot be decorated by @Tx"
                    )
                }
                return true
            }
        }
        return false
    }

    private fun validateType(declaration: KSClassDeclaration) {
        if (declaration.classKind != ClassKind.CLASS) {
            throw MetaException(
                declaration,
                "The type uses @Tx must be class"
            )
        }
        if (declaration.modifiers.contains(Modifier.DATA)) {
            throw MetaException(
                declaration,
                "The class uses @Tx cannot be data class"
            )
        }
        if (declaration.modifiers.contains(Modifier.SEALED)) {
            throw MetaException(
                declaration,
                "The class uses @Tx cannot be sealed class"
            )
        }
        if (!declaration.modifiers.contains(Modifier.OPEN)) {
            throw MetaException(
                declaration,
                "The class uses @Tx must be open"
            )
        }
        if (declaration.typeParameters.isNotEmpty()) {
            throw MetaException(
                declaration,
                "The current version does not yet support the use of generics for types annotated with @Tx"
            )
        }
        for (superTypeRef in declaration.superTypes) {
            val superType = superTypeRef.fastResolve()
            val superDeclaration = superType.declaration
            if (superDeclaration is KSClassDeclaration &&
                superDeclaration.classKind == ClassKind.CLASS &&
                !superType.isAssignableFrom(ctx.resolver.builtIns.anyType)) {
                throw MetaException(
                    declaration,
                    "The current version does not yet support the use of inheritance for types annotated with @Tx"
                )
            }
        }
    }
}