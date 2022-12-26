package org.babyfish.jimmer.example.kt.sql.dal

import org.babyfish.jimmer.example.kt.sql.model.TreeNode
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.fetcher.Fetcher

interface TreeNodeRepository : KRepository<TreeNode, Long> {

    fun findByParentIsNullAndName(
        name: String?,
        fetcher: Fetcher<TreeNode>?
    ): List<TreeNode>
}