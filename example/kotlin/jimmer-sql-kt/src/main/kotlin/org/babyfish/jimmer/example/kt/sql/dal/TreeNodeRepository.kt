package org.babyfish.jimmer.example.kt.sql.dal

import org.babyfish.jimmer.example.kt.sql.model.TreeNode
import org.babyfish.jimmer.example.kt.sql.model.name
import org.babyfish.jimmer.example.kt.sql.model.parent
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.table.isNull

@JvmDefaultWithCompatibility
interface TreeNodeRepository : KRepository<TreeNode, Long> {

    fun findRootNodes(
        name: String?,
        fetcher: Fetcher<TreeNode>?
    ): List<TreeNode> =
        sql
            .createQuery(TreeNode::class) {
                where(table.parent.isNull())
                name?.takeIf { it.isNotEmpty() }?.let {
                    where(table.name eq it)
                }
                select(table.fetch(fetcher))
            }
            .execute()
}