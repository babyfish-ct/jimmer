package org.babyfish.jimmer.ksp.tuple

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.annotations
import org.babyfish.jimmer.ksp.fullName
import org.babyfish.jimmer.sql.TypedTuple

class TypedTupleProcessor(
    private val ctx: Context,
    private val delayedClientTypeNames: Collection<String>?
) {
    fun process(): List<KSClassDeclaration> {
        var processedDeclarations = mutableListOf<KSClassDeclaration>()
        for (file in ctx.resolver.getAllFiles()) {
            for (declaration in file.declarations) {
                if (declaration.annotations { it.fullName == TypedTuple::class.qualifiedName }.isNotEmpty()) {
                    generate(declaration as KSClassDeclaration)
                    processedDeclarations += declaration
                }
            }
        }
        if (delayedClientTypeNames != null) {
            for (delayedClientTypeName in delayedClientTypeNames) {
                val declaration = ctx.resolver.getClassDeclarationByName(delayedClientTypeName)!!
                generate(declaration)
                processedDeclarations += declaration
            }
        }
        return processedDeclarations
    }

    private fun generate(declaration: KSClassDeclaration) {
        TypedTupleGenerator(ctx, declaration).generate()
    }
}