package org.babyfish.jimmer.spring.kotlin

import org.babyfish.jimmer.Page
import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.spring.kotlin.dto.TreeNodeView2
import org.babyfish.jimmer.spring.repository.DynamicParam
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.table.isNull
import org.springframework.data.domain.Pageable

interface TreeNodeRepository : KRepository<TreeNode, Long> {

    fun findByParentIsNullAndNameOrderByIdAsc(
        @DynamicParam name: String?,
        fetcher: Fetcher<TreeNode>?
    ): List<TreeNode>

    fun findRootNodes(): List<TreeNode> =
        sql.createQuery(TreeNode::class) {
            where(table.`parent?`.isNull())
            select(table)
        }.execute()

    fun findPage(pageIndex: Int, pageSize: Int): Page<TreeNode> =
        sql
            .createQuery(TreeNode::class) {
                orderBy(table.name.asc(), table.id.asc())
                select(table)
            }
            .fetchPage(pageIndex, pageSize)

    fun find(pageable: Pageable): Page<TreeNode>

    fun findByNameAndParentId(name: String, parentId: Long): TreeNode?

    fun findByNameLike(name: String?): List<TreeNodeView2>

    fun find(specification: Specification<TreeNode>): List<TreeNode>
}