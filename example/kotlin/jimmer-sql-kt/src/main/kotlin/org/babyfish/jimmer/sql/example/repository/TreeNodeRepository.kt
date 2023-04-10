package org.babyfish.jimmer.sql.example.repository

import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.example.model.TreeNode
import org.babyfish.jimmer.sql.fetcher.Fetcher

interface TreeNodeRepository : KRepository<TreeNode, Long> {

    fun findByParentIsNullAndName(
        name: String?,
        fetcher: Fetcher<TreeNode>?
    ): List<TreeNode>
}