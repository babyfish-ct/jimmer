package org.babyfish.jimmer.meta.impl.dto.ast.spi

interface BaseProp {
    val name: String
    val isNullable: Boolean
    val isList: Boolean
    val isTransient: Boolean
    fun hasTransientResolver(): Boolean
}