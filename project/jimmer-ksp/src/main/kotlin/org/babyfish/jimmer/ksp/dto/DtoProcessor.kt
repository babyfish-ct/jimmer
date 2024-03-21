package org.babyfish.jimmer.ksp.dto

import com.google.devtools.ksp.getClassDeclarationByName
import org.babyfish.jimmer.dto.compiler.DtoAstException
import org.babyfish.jimmer.dto.compiler.DtoType
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.KspDtoCompiler
import org.babyfish.jimmer.ksp.annotation
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType
import org.babyfish.jimmer.sql.Entity

class DtoProcessor(
    private val ctx: Context,
    private val dtoDirs: Collection<String>,
    private val dtoMutable: Boolean
) {
    fun process(): Boolean {
        val dtoTypeMap = findDtoTypeMap()
        generateDtoTypes(dtoTypeMap)
        return dtoTypeMap.isNotEmpty()
    }

    private fun findDtoTypeMap(): Map<ImmutableType, MutableList<DtoType<ImmutableType, ImmutableProp>>> {
        val dtoTypeMap = mutableMapOf<ImmutableType, MutableList<DtoType<ImmutableType, ImmutableProp>>>()
        val dtoCtx = DtoContext(ctx.resolver.getAllFiles().firstOrNull(), dtoDirs)
        val immutableTypeMap = mutableMapOf<KspDtoCompiler, ImmutableType>()
        for (dtoFile in dtoCtx.dtoFiles) {
            val compiler = try {
                KspDtoCompiler(dtoFile, ctx.resolver)
            } catch (ex: DtoAstException) {
                throw DtoException(
                    "Failed to parse \"" +
                        dtoFile.absolutePath +
                        "\": " +
                        ex.message,
                    ex
                )
            } catch (ex: Throwable) {
                throw DtoException(
                    "Failed to read \"" +
                        dtoFile.absolutePath +
                        "\": " +
                        ex.message,
                    ex
                )
            }
            val classDeclaration = ctx.resolver.getClassDeclarationByName(compiler.sourceTypeName)
            if (classDeclaration === null) {
                throw DtoException(
                    "Failed to parse \"" +
                        dtoFile.absolutePath +
                        "\": No entity type \"" +
                        compiler.sourceTypeName +
                        "\""
                )
            }
            if (!ctx.include(classDeclaration)) {
                continue
            }
            if (classDeclaration.annotation(Entity::class) == null) {
                throw DtoException(
                    "Failed to parse \"" +
                        dtoFile.absolutePath +
                        "\": the \"" +
                        compiler.sourceTypeName +
                        "\" is not decorated by \"@" +
                        Entity::class.qualifiedName +
                        "\""
                )
            }
            immutableTypeMap[compiler] = ctx.typeOf(classDeclaration)
        }
        ctx.resolve()
        for ((compiler, immutableType) in immutableTypeMap) {
            dtoTypeMap.computeIfAbsent(immutableType) {
                mutableListOf()
            } += compiler.compile(immutableType)
        }
        return dtoTypeMap
    }

    private fun generateDtoTypes(
        dtoTypeMap: Map<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>>
    ) {
        val allFiles = ctx.resolver.getAllFiles().toList()
        for (dtoTypes in dtoTypeMap.values) {
            for (dtoType in dtoTypes) {
                DtoGenerator(ctx, dtoType, dtoMutable, ctx.environment.codeGenerator).generate(allFiles)
            }
        }
    }
}