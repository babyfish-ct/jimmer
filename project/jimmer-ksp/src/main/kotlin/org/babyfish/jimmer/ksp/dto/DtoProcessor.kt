package org.babyfish.jimmer.ksp.dto

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSClassDeclaration
import org.babyfish.jimmer.Input
import org.babyfish.jimmer.View
import org.babyfish.jimmer.dto.compiler.*
import org.babyfish.jimmer.dto.compiler.Anno.EnumValue
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.KspDtoCompiler
import org.babyfish.jimmer.ksp.client.DocMetadata
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType
import org.babyfish.jimmer.ksp.util.GenericParser
import org.babyfish.jimmer.ksp.util.fastResolve

class DtoProcessor(
    private val ctx: Context,
    private val mutable: Boolean,
    private val dtoDirs: Collection<String>,
    private val defaultNullableInputModifier: DtoModifier
) {
    fun process(): Boolean {
        val dtoTypeMap = findDtoTypeMap()
        generateDtoTypes(dtoTypeMap)
        return dtoTypeMap.isNotEmpty()
    }

    private fun findDtoTypeMap(): Map<ImmutableType, MutableList<DtoType<ImmutableType, ImmutableProp>>> {
        val dtoTypeMap = mutableMapOf<ImmutableType, MutableList<DtoType<ImmutableType, ImmutableProp>>>()
        val dtoCtx = DtoContext(ctx.resolver.getAllFiles().firstOrNull(), dtoDirs)
        val compilers = mutableListOf<KspDtoCompiler>()
        for (dtoFile in dtoCtx.dtoFiles) {
            val compiler = try {
                KspDtoCompiler(dtoFile, ctx, defaultNullableInputModifier)
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
            compilers += compiler
        }
        for (dtoTypes in DtoCompiler.compileAll(compilers, ctx::includeDtoTarget).values) {
            for (dtoType in dtoTypes) {
                dtoTypeMap.computeIfAbsent(dtoType.baseType) {
                    mutableListOf()
                } += dtoType
            }
        }
        DtoTypeLinker.link(dtoTypeMap.values.flatten(), ::resolveDtoType)
        ctx.resolve()
        return dtoTypeMap
    }

    private fun resolveDtoType(qualifiedName: String): DtoTypeInfo<ImmutableType>? {
        val declaration = ctx.resolver.getClassDeclarationByName(qualifiedName) ?: return null
        val inputType = ctx.resolver
            .getClassDeclarationByName(Input::class.qualifiedName!!)!!
            .asStarProjectedType()
        val viewType = ctx.resolver
            .getClassDeclarationByName(View::class.qualifiedName!!)!!
            .asStarProjectedType()
        val kind: DtoTypeKind
        val superName: String
        val type = declaration.asStarProjectedType()
        if (inputType.isAssignableFrom(type)) {
            kind = DtoTypeKind.INPUT
            superName = Input::class.qualifiedName!!
        } else if (viewType.isAssignableFrom(type)) {
            kind = DtoTypeKind.VIEW
            superName = View::class.qualifiedName!!
        } else {
            return null
        }
        val baseDeclaration = GenericParser(
            "reusable DTO",
            declaration,
            superName
        ).parse().arguments[0].type!!.fastResolve().declaration as? KSClassDeclaration
            ?: throw DtoException(
                "The entity type argument of reusable DTO type \"$qualifiedName\" " +
                    "is not an immutable type"
            )
        if (ctx.typeAnnotationOf(baseDeclaration) == null) {
            throw DtoException(
                "The entity type argument of reusable DTO type \"$qualifiedName\" " +
                    "is not an immutable type"
            )
        }
        return DtoTypeInfo(ctx.typeOf(baseDeclaration), kind)
    }

    private fun generateDtoTypes(
        dtoTypeMap: Map<ImmutableType, List<DtoType<ImmutableType, ImmutableProp>>>
    ) {
        val allFiles = ctx.resolver.getAllFiles().toList()
        val docMetadata = DocMetadata(ctx)
        for (dtoTypes in dtoTypeMap.values) {
            for (dtoType in dtoTypes) {
                val mutable = dtoType.annotations.firstOrNull {
                    it.qualifiedName == "org.babyfish.jimmer.kt.dto.KotlinDto"
                }?.let {
                    val value = it.valueMap["immutability"] as EnumValue
                    when (value.constant) {
                        "IMMUTABLE" -> false
                        "MUTABLE" -> true
                        else -> null
                    }
                } ?: mutable
                DtoGenerator(ctx, docMetadata, mutable, dtoType, ctx.environment.codeGenerator).generate(allFiles)
            }
        }
    }
}
