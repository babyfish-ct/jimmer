package org.babyfish.jimmer.ksp

import org.babyfish.jimmer.dto.compiler.DtoCompiler
import org.babyfish.jimmer.ksp.meta.ImmutableProp
import org.babyfish.jimmer.ksp.meta.ImmutableType

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
}