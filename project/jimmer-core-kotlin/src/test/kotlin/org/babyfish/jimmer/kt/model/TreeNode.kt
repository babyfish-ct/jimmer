package org.babyfish.jimmer.kt.model

import org.babyfish.jimmer.Immutable

@Immutable
interface TreeNode {
    val name: String
    val parent: TreeNode?
    val childNodes: List<TreeNode>
}