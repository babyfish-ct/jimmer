package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.fetcher.RecursionStrategy
import org.babyfish.jimmer.sql.kt.model.TreeNode

class TreeNodeRecursiveStrategy : RecursionStrategy<TreeNode> {

    override fun isRecursive(args: RecursionStrategy.Args<TreeNode>): Boolean =
        args.entity.name != "Clothing"
}