package org.babyfish.jimmer.ksp

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.STRING
import org.babyfish.jimmer.dto.compiler.DtoCompiler
import org.babyfish.jimmer.dto.compiler.DtoFile
import org.babyfish.jimmer.dto.compiler.DtoModifier
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType
import org.babyfish.jimmer.sql.GeneratedValue

class KspDtoCompiler(
    dtoFile: DtoFile,
    private val resolver: Resolver,
    private val defaultNullableInputModifier: DtoModifier
) : DtoCompiler<ImmutableType, ImmutableProp>(dtoFile) {

    override fun getDefaultNullableInputModifier(): DtoModifier =
        defaultNullableInputModifier

    override fun getSuperTypes(baseType: ImmutableType): Collection<ImmutableType> =
        baseType.superTypes

    override fun getDeclaredProps(baseType: ImmutableType): Map<String, ImmutableProp> =
        baseType.declaredProperties

    override fun getProps(baseType: ImmutableType): Map<String, ImmutableProp> =
        baseType.properties

    override fun getTargetType(baseProp: ImmutableProp): ImmutableType? =
        baseProp.targetType

    override fun isGeneratedValue(baseProp: ImmutableProp): Boolean =
        baseProp.annotation(GeneratedValue::class) !== null

    override fun getEnumConstants(baseProp: ImmutableProp): List<String>? =
        (baseProp.resolvedType.declaration as? KSClassDeclaration)?.let { decl ->
            decl.takeIf { it.classKind == ClassKind.ENUM_CLASS }?.let { enumDecl ->
                enumDecl
                    .declarations
                    .filter {
                        it is KSClassDeclaration && it.classKind == ClassKind.ENUM_ENTRY
                    }
                    .map { it.simpleName.asString() }
                    .toList()
            }
        }

    override fun isSameType(baseProp1: ImmutableProp, baseProp2: ImmutableProp): Boolean =
        baseProp1.clientClassName.copy(nullable = false) == baseProp2.clientClassName.copy(nullable = false)

    override fun isStringProp(baseProp: ImmutableProp): Boolean =
        baseProp.typeName(overrideNullable = false) == STRING

    override fun getGenericTypeCount(qualifiedName: String): Int? =
        resolver.getClassDeclarationByName(qualifiedName)?.typeParameters?.size
}