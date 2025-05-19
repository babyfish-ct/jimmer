package org.babyfish.jimmer.sql.kt.api

/**
 * User defined class
 */
data class Tree<T>(
    /**
     * Field 1
     */
    val name: String,
    /**
     * Field 2
     */
    val children: List<Tree<T>>
)