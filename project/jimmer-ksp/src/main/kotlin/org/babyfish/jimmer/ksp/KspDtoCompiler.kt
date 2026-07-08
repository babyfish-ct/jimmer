package org.babyfish.jimmer.ksp

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import org.babyfish.jimmer.dto.compiler.*
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType
import org.babyfish.jimmer.sql.GeneratedValue
import java.math.BigDecimal
import java.math.BigInteger

class KspDtoCompiler(
    dtoFile: DtoFile,
    private val ctx: Context,
    private val defaultNullableInputModifier: DtoModifier
) : DtoCompiler<ImmutableType, ImmutableProp>(dtoFile) {

    private val resolver: Resolver = ctx.resolver

    override fun getDefaultNullableInputModifier(): DtoModifier =
        defaultNullableInputModifier

    override fun getSuperTypes(baseType: ImmutableType): Collection<ImmutableType> =
        baseType.superTypes

    override fun getType(qualifiedName: String): ImmutableType? =
        ctx.types.firstOrNull { it.qualifiedName == qualifiedName }
            ?: resolver
                .getClassDeclarationByName(qualifiedName)
                ?.takeIf { ctx.typeAnnotationOf(it) !== null }
                ?.let {
                    ctx.typeOf(it).also { ctx.resolve() }
                }

    override fun getDirectSubTypes(baseType: ImmutableType): Collection<ImmutableType> =
        ctx.types
            .filter { it.primarySuperType?.qualifiedName == baseType.qualifiedName }
            .sortedBy { it.qualifiedName }

    override fun isSameType(baseType1: ImmutableType, baseType2: ImmutableType): Boolean =
        baseType1.qualifiedName == baseType2.qualifiedName

    override fun isInstantiable(baseType: ImmutableType): Boolean =
        baseType.isInstantiable

    override fun getDeclaredProps(baseType: ImmutableType): Map<String, ImmutableProp> =
        baseType.declaredProperties

    override fun getProps(baseType: ImmutableType): Map<String, ImmutableProp> =
        baseType.properties

    override fun getTargetType(baseProp: ImmutableProp): ImmutableType? =
        baseProp.targetType

    override fun getIdProp(baseType: ImmutableType): ImmutableProp? =
        baseType.idProp

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
        SIMPLE_PROP_TYPE_MAP[baseProp.typeName().copy(nullable = false)] ?: SimplePropType.NONE

    override fun getSimplePropType(pathNode: PropConfig.PathNode<ImmutableProp>): SimplePropType =
        SIMPLE_PROP_TYPE_MAP[
            if (pathNode.isAssociatedId) {
                pathNode.prop.targetType!!.idProp!!.typeName().copy(nullable = false)
            } else {
                pathNode.prop.typeName().copy(nullable = false)
            }
        ] ?: error(pathNode.prop.typeName())

    override fun getGenericTypeCount(qualifiedName: String): Int? =
        resolver.getClassDeclarationByName(qualifiedName)?.typeParameters?.size

    companion object {
        @JvmStatic
        private val SIMPLE_PROP_TYPE_MAP = mapOf(
            BOOLEAN to SimplePropType.BOOLEAN,
            BYTE to SimplePropType.BYTE,
            SHORT to SimplePropType.SHORT,
            INT to SimplePropType.INT,
            LONG to SimplePropType.LONG,
            FLOAT to SimplePropType.FLOAT,
            DOUBLE to SimplePropType.DOUBLE,

            BigInteger::class.asTypeName().copy(nullable = false) to SimplePropType.BIG_INTEGER,
            BigDecimal::class.asTypeName().copy(nullable = false) to SimplePropType.BIG_DECIMAL,

            String::class.asTypeName().copy(nullable = false) to SimplePropType.STRING,
        )
    }
}
