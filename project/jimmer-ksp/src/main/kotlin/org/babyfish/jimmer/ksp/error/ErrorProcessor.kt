package org.babyfish.jimmer.ksp.error

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.annotation

class ErrorProcessor(
    private val ctx: Context,
    private val checkedException: Boolean
) {
    fun process(): Boolean {
        val errorTypes = findErrorTypes()
        generateErrorTypes(errorTypes)
        return errorTypes.isNotEmpty()
    }

    private fun findErrorTypes(): List<KSClassDeclaration> =
        ctx
            .resolver
            .getNewFiles()
            .flatMap { file ->
                file
                    .declarations
                    .filterIsInstance<KSClassDeclaration>()
                    .filter{
                        it.classKind == ClassKind.ENUM_CLASS &&
                            it.annotation(ErrorFamily::class) != null &&
                            ctx.include(it)
                    }
            }
            .toList()

    private fun generateErrorTypes(declarations: Collection<KSClassDeclaration>) {
        val allFiles = ctx.resolver.getNewFiles().toList()
        for (declaration in declarations) {
            ErrorGenerator(ctx, declaration, checkedException, ctx.environment.codeGenerator).generate(allFiles)
        }
    }
}