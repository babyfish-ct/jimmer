package org.babyfish.jimmer.sql.kt.query.tuple

import org.babyfish.jimmer.sql.TypedTuple
import org.babyfish.jimmer.sql.kt.model.TreeNode

@TypedTuple
data class RecursiveTuple(
    val node: TreeNode,
    val depth: Int
)
