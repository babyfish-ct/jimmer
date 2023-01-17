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
    fun isNullable(input: Boolean): Boolean =
        when {
            isOptional -> true
            immutableProp.isNullable ->
                if (input) {
                    !immutableProp.isInputNotNull
                } else {
                    true
                }
            else -> false
        }
}