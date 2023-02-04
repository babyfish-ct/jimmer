package org.babyfish.jimmer.dto.compiler.spi

interface BaseProp {
    val name: String
    val isNullable: Boolean
    val isList: Boolean
    val isTransient: Boolean
    fun hasTransientResolver(): Boolean
}