package org.babyfish.jimmer.dto.compiler.spi

interface BaseProp {
    val name: String
    val isNullable: Boolean
    val isList: Boolean
    val isFormula: Boolean
    val isTransient: Boolean
    val idViewBaseProp: BaseProp?
    val manyToManyViewBaseProp: BaseProp?
    val isId: Boolean
    val isKey: Boolean
    val isRecursive: Boolean
    val isEmbedded: Boolean
    fun isAssociation(entityLevel: Boolean): Boolean
    fun hasTransientResolver(): Boolean
}