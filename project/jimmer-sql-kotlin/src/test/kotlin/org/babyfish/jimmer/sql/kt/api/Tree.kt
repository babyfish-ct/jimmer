package org.babyfish.jimmer.sql.kt.api

data class Tree<T>(
    val name: String,
    val children: List<Tree<T>>
)