package org.babyfish.jimmer.ksp

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.STRING
import org.babyfish.jimmer.dto.compiler.DtoCompiler
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType
import org.babyfish.jimmer.sql.GeneratedValue

class KspDtoCompiler(
    immutableType: ImmutableType,
    dtoFilePath: String
) : DtoCompiler<ImmutableType, ImmutableProp>(immutableType, dtoFilePath) {

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
        (baseProp.propDeclaration.type.resolve().declaration as? KSClassDeclaration)?.let { decl ->
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
        baseProp1.typeName(overrideNullable = false) == baseProp2.typeName(overrideNullable = false)

    override fun isStringProp(baseProp: ImmutableProp): Boolean =
        baseProp.typeName(overrideNullable = false) == STRING
}