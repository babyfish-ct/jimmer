package org.babyfish.jimmer.spring.kotlin

import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.table.isNull

interface TreeNodeRepository : KRepository<TreeNode, Long> {

    fun findByParentIsNullAndNameOrderByIdAsc(
        name: String?,
        fetcher: Fetcher<TreeNode>?
    ): List<TreeNode>

    fun findRootNodes(): List<TreeNode> =
        sql.createQuery(TreeNode::class) {
            where(table.parent.isNull())
            select(table)
        }.execute()
}