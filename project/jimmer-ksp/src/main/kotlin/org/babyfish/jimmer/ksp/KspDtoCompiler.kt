package org.babyfish.jimmer.ksp

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import org.babyfish.jimmer.dto.compiler.DtoCompiler
import org.babyfish.jimmer.dto.compiler.DtoFile
import org.babyfish.jimmer.dto.compiler.DtoModifier
import org.babyfish.jimmer.dto.compiler.SimplePropType
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType
import org.babyfish.jimmer.sql.GeneratedValue
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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

    override fun getSimplePropType(baseProp: ImmutableProp): SimplePropType =
        SIMPLE_PROP_TYPE_MAP[baseProp.typeName()] ?: SimplePropType.NONE

    override fun getGenericTypeCount(qualifiedName: String): Int? =
        resolver.getClassDeclarationByName(qualifiedName)?.typeParameters?.size

    companion object {
        @JvmStatic
        private val SIMPLE_PROP_TYPE_MAP = mapOf(
            BOOLEAN to SimplePropType.BOOLEAN,
            BOOLEAN.copy(nullable = true) to SimplePropType.BOOLEAN,
            BYTE to SimplePropType.BYTE,
            BYTE.copy(nullable = true) to SimplePropType.BYTE,
            SHORT to SimplePropType.SHORT,
            SHORT.copy(nullable = true) to SimplePropType.SHORT,
            INT to SimplePropType.INT,
            INT.copy(nullable = true) to SimplePropType.INT,
            LONG to SimplePropType.LONG,
            LONG.copy(nullable = true) to SimplePropType.LONG,
            FLOAT to SimplePropType.FLOAT,
            FLOAT.copy(nullable = true) to SimplePropType.FLOAT,
            DOUBLE to SimplePropType.DOUBLE,
            DOUBLE.copy(nullable = true) to SimplePropType.DOUBLE,

            BigInteger::class.asTypeName() to SimplePropType.BIG_INTEGER,
            BigInteger::class.asTypeName().copy(nullable = true) to SimplePropType.BIG_INTEGER,
            BigDecimal::class.asTypeName() to SimplePropType.BIG_DECIMAL,
            BigDecimal::class.asTypeName().copy(nullable = true) to SimplePropType.BIG_DECIMAL,

            String::class.asTypeName() to SimplePropType.STRING,
            String::class.asTypeName().copy(nullable = true) to SimplePropType.STRING,
        )
    }
}