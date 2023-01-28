package org.babyfish.jimmer.ksp.meta

data class StaticDeclaration(
    val immutableType: ImmutableType,
    val alias: String,
    val topLevelName: String,
    val allOptional: Boolean
)