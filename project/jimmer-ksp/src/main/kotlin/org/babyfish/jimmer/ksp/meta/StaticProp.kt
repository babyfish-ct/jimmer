package org.babyfish.jimmer.ksp.meta

data class StaticProp(
    val immutableProp: ImmutableProp,
    val alias: String,
    val name: String,
    val isEnabled: Boolean,
    val isOptional: Boolean,
    val isIdOnly: Boolean,
    val targetAlias: String,
    val target: StaticDeclaration? = null
) {
    val isNullable: Boolean
        get() = isOptional || immutableProp.isNullable
}