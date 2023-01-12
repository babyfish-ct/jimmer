package org.babyfish.jimmer.ksp.meta

import org.babyfish.jimmer.pojo.AutoScalarStrategy

data class StaticDeclaration(
    val immutableType: ImmutableType,
    val alias: String,
    val topLevelName: String,
    val autoScalarStrategy: AutoScalarStrategy,
    val allOptional: Boolean
)