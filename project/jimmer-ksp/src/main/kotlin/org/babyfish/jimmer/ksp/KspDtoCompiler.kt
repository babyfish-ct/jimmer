package org.babyfish.jimmer.ksp

import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType
import org.babyfish.jimmer.meta.impl.dto.ast.DtoCompiler

class KspDtoCompiler(
    private val immutableType: ImmutableType
) : DtoCompiler<ImmutableType, ImmutableProp>(immutableType) {

    override fun isEntity(baseType: ImmutableType): Boolean =
        baseType.isEntity

    override fun getSuperType(baseType: ImmutableType): ImmutableType? =
        baseType.superType

    override fun getDeclaredProps(baseType: ImmutableType): Map<String, ImmutableProp> =
        baseType.declaredProperties

    override fun getProps(baseType: ImmutableType): Map<String, ImmutableProp> =
        baseType.properties

    override fun isId(baseProp: ImmutableProp): Boolean =
        baseProp.isId

    override fun isKey(baseProp: ImmutableProp): Boolean =
        baseProp.isKey

    override fun getTargetType(baseProp: ImmutableProp): ImmutableType? =
        baseProp.targetType
}