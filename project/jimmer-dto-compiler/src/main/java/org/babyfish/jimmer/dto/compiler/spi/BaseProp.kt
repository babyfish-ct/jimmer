package org.babyfish.jimmer.dto.compiler.spi

interface BaseProp {
    val name: String
    val isNullable: Boolean
    val isList: Boolean
    val isFormula: Boolean
    val isTransient: Boolean
    val isView: Boolean
    fun hasTransientResolver(): Boolean
}